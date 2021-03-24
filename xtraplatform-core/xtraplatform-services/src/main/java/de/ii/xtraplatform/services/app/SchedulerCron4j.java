/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.app;

import de.ii.xtraplatform.runtime.domain.LogContext;
import de.ii.xtraplatform.services.domain.Scheduler;
import de.ii.xtraplatform.services.domain.Task;
import de.ii.xtraplatform.services.domain.TaskQueue;
import de.ii.xtraplatform.services.domain.TaskStatus;
import it.sauronsoftware.cron4j.SchedulerListener;
import it.sauronsoftware.cron4j.TaskExecutor;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.AmountFormats;

/** @author zahnen */
@Component
@Provides
@Instantiate
/*@Wbp(
filter = "(objectClass=de.ii.xtraplatform.services.domain.SchedulerTask)",
onArrival = "onTaskArrival",
onDeparture = "onTaskDeparture")*/
public class SchedulerCron4j implements Scheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerCron4j.class);

  private final BundleContext context;

  private final it.sauronsoftware.cron4j.Scheduler scheduler;

  public SchedulerCron4j(@Context BundleContext context) {
    this.context = context;
    this.scheduler = new it.sauronsoftware.cron4j.Scheduler();
    scheduler.setDaemon(true);
    // needed to suppress exception printing to stdout
    scheduler.addSchedulerListener(
        new SchedulerListener() {
          @Override
          public void taskLaunching(TaskExecutor executor) {}

          @Override
          public void taskSucceeded(TaskExecutor executor) {}

          @Override
          public void taskFailed(TaskExecutor executor, Throwable exception) {}
        });
  }

  @Validate
  public void start() {
    scheduler.start();
  }

  @Invalidate
  public void stop() {
    scheduler.stop();
  }
  /*
  public synchronized void onTaskArrival(ServiceReference<SchedulerTask> ref) {
    SchedulerTask task = context.getService(ref);

    String id = scheduler.schedule(task.getPattern(), task.getTask());
    task.setId(id);
  }

  public synchronized void onTaskDeparture(ServiceReference<SchedulerTask> ref) {
    SchedulerTask task = context.getService(ref);

    scheduler.deschedule(task.getId());
  }*/

  @Override
  public TaskStatus launch(Task task) {

    final TaskExecutor taskExecutor = scheduler.launch(new TaskCron4j(task));

    return new TaskStatusCron4j(task.getId(), task.getLabel(), taskExecutor);
  }

  @Override
  public TaskQueue createQueue(String id) {
    return new TaskQueue() {
      private final BlockingQueue<Pair<Task, CompletableFuture<TaskStatus>>> queue =
          new LinkedBlockingQueue<>();
      private TaskStatus currentTask;

      @Override
      public synchronized CompletableFuture<TaskStatus> launch(Task task) {
        TaskStatus runningTask = this.currentTask;
        if (Objects.nonNull(runningTask)
            && Objects.equals(runningTask.getId(), task.getId())
            && Objects.equals(runningTask.getLabel(), task.getLabel())) {
          LOGGER.debug("Ignoring task {} for {}, already running", task.getLabel(), task.getId());
          return CompletableFuture.failedFuture(new IllegalArgumentException());
        }
        if (getFutureTasks().stream()
            .anyMatch(
                futureTask ->
                    Objects.equals(futureTask.getId(), task.getId())
                        && Objects.equals(futureTask.getLabel(), task.getLabel()))) {
          LOGGER.debug("Ignoring task {} for {}, already in queue", task.getLabel(), task.getId());
          return CompletableFuture.failedFuture(new IllegalArgumentException());
        }

        // LOGGER.debug("Queuing task {}", task.getLabel());
        final CompletableFuture<TaskStatus> taskStatusCompletableFuture = new CompletableFuture<>();

        queue.offer(new ImmutablePair<>(task, taskStatusCompletableFuture));

        checkQueue(false);

        return taskStatusCompletableFuture;
      }

      @Override
      public CompletableFuture<TaskStatus> launch(Task task, long delay) {
        CompletableFuture<TaskStatus> completableFuture = new CompletableFuture<>();
        ForkJoinPool.commonPool()
            .execute(
                () -> {
                  try {
                    Thread.sleep(delay);
                  } catch (InterruptedException e) {
                    // ignore
                  }
                  launch(task).thenAccept(completableFuture::complete);
                });
        return completableFuture;
      }

      @Override
      public void remove(Task task) {
        List<Pair<Task, CompletableFuture<TaskStatus>>> toRemove =
            queue.stream()
                .filter(entry -> Objects.equals(task, entry.getLeft()))
                .collect(Collectors.toList());
        toRemove.forEach(
            o -> {
              boolean removed = queue.remove(o);
              if (removed) {
                LOGGER.debug("REMOVED TASK {} -> {}", o.getLeft().getLabel(), o.getLeft().getId());
              }
            });
      }

      @Override
      public List<Task> getFutureTasks() {
        return queue.stream().map(Pair::getLeft).collect(Collectors.toList());
      }

      @Override
      public Optional<TaskStatus> getCurrentTask() {
        return Optional.ofNullable(currentTask);
      }

      private synchronized void checkQueue(boolean currentIsDone) {
        if (currentIsDone) {
          this.currentTask = null;
        }
        if (Objects.isNull(currentTask) || currentTask.isDone()) {
          Thread.currentThread().setName("bg-task-1");
          final Pair<Task, CompletableFuture<TaskStatus>> task = queue.poll();
          if (Objects.nonNull(task)) {
            task.getLeft().logContext();
            LOGGER.info("{} started", task.getLeft().getLabel());
            this.currentTask = SchedulerCron4j.this.launch(task.getLeft());
            task.getRight().complete(currentTask);
            currentTask.onChange(
                (progress, message) -> {
                  if (LOGGER.isDebugEnabled()) {
                    Thread.currentThread().setName("bg-task-1");
                    LOGGER.debug(
                        "{}: {} [{}]",
                        task.getLeft().getLabel(),
                        message,
                        new DecimalFormat("#%").format(progress));
                  }
                },
                1000);
            currentTask.onDone(
                (optionalThrowable) -> {
                  Thread.currentThread().setName("bg-task-1");

                  if (optionalThrowable.isPresent()) {
                    Throwable throwable = optionalThrowable.get();
                    String msg1 = throwable.getMessage();
                    if (Objects.isNull(msg1)) {
                      msg1 =
                          String.format(
                              "%s at %s",
                              throwable.getClass().getSimpleName(),
                              throwable.getStackTrace()[0].toString());
                    }
                    String msg2 =
                        Objects.nonNull(throwable.getCause())
                            ? throwable.getCause().getMessage()
                            : "";
                    if (Objects.isNull(msg2)) {
                      msg2 =
                          String.format(
                              "%s at %s",
                              throwable.getCause().getClass().getSimpleName(),
                              throwable.getCause().getStackTrace()[0].toString());
                    }

                    LOGGER.error("{} failed: {} Cause: {}", task.getLeft().getLabel(), msg1, msg2);

                    if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
                      LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace:", throwable);
                    }
                  } else {
                    String time = pretty(currentTask.getEndTime() - currentTask.getStartTime());

                    LOGGER.info("{} finished in {}", task.getLeft().getLabel(), time);
                  }

                  checkQueue(true);
                });
          }
        }
      }

      private String pretty(long milliseconds) {
        Duration d = Duration.ofSeconds(milliseconds / 1000);
        return AmountFormats.wordBased(d, Locale.ENGLISH);
      }
    };
  }
}
