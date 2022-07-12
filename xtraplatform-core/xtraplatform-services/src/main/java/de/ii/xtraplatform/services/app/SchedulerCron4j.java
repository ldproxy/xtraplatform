/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.LogContext;
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
import java.util.stream.IntStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.AmountFormats;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class SchedulerCron4j implements Scheduler, AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerCron4j.class);

  private final it.sauronsoftware.cron4j.Scheduler scheduler;

  @Inject
  public SchedulerCron4j() {
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

  @Override
  public void onStart() {
    scheduler.start();
  }

  @Override
  public void onStop() {
    scheduler.stop();
  }

  @Override
  public TaskStatus launch(Task task) {
    TaskCron4j taskCron4j = new TaskCron4j(task, 1);
    final TaskExecutor taskExecutor = scheduler.launch(taskCron4j);

    return new TaskStatusCron4j(taskCron4j, taskExecutor);
  }

  @Override
  public String schedule(Runnable runnable, String cronPattern) {
    return scheduler.schedule(cronPattern, runnable);
  }

  @Override
  public void deschedule(String jobId) {
    scheduler.deschedule(jobId);
  }

  @Override
  public TaskQueue createQueue(String id, int maxConcurrentTasks) {
    return new TaskQueue() {
      private final BlockingQueue<Pair<Task, CompletableFuture<TaskStatus>>> queue =
          new LinkedBlockingQueue<>();
      private final BlockingQueue<TaskStatus> currentTasks =
          new LinkedBlockingQueue<>(maxConcurrentTasks);
      private final BlockingQueue<Integer> threadNumbers =
          new LinkedBlockingQueue<>(
              IntStream.rangeClosed(1, maxConcurrentTasks).boxed().collect(Collectors.toList()));

      @Override
      public synchronized CompletableFuture<TaskStatus> launch(Task task) {
        Thread.currentThread().setName("bg-task-0");
        task.logContext();
        cleanup();

        for (TaskStatus runningTask : currentTasks) {
          if (Objects.nonNull(runningTask)
              && Objects.equals(runningTask.getId(), task.getId())
              && Objects.equals(runningTask.getLabel(), task.getLabel())) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(
                  "Ignoring task '{}' for '{}', already running", task.getLabel(), task.getId());
            }
            return CompletableFuture.failedFuture(new IllegalArgumentException());
          }
        }

        if (getFutureTasks().stream()
            .anyMatch(
                futureTask ->
                    Objects.equals(futureTask.getId(), task.getId())
                        && Objects.equals(futureTask.getLabel(), task.getLabel()))) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                "Ignoring task '{}' for '{}', already in queue", task.getLabel(), task.getId());
          }
          return CompletableFuture.failedFuture(new IllegalArgumentException());
        }

        // LOGGER.debug("Queuing task {}", task.getLabel());
        final CompletableFuture<TaskStatus> taskStatusCompletableFuture = new CompletableFuture<>();

        queue.offer(new ImmutablePair<>(task, taskStatusCompletableFuture));

        checkQueue();

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
                if (LOGGER.isTraceEnabled()) {
                  LOGGER.trace(
                      "REMOVED TASK {} -> {}", o.getLeft().getLabel(), o.getLeft().getId());
                }
              }
            });
      }

      @Override
      public List<Task> getFutureTasks() {
        return queue.stream().map(Pair::getLeft).collect(Collectors.toList());
      }

      // TODO: list of currentTasks
      @Override
      public Optional<TaskStatus> getCurrentTask() {
        return Optional.ofNullable(currentTasks.peek());
      }

      private synchronized void checkQueue() {
        cleanup();

        if (currentTasks.remainingCapacity() > 0) {
          final Pair<Task, CompletableFuture<TaskStatus>> task = queue.poll();

          if (Objects.nonNull(task)) {
            if (task.getLeft().getMaxPartials() > 1 && currentTasks.remainingCapacity() > 1) {
              int maxPartials =
                  Math.min(currentTasks.remainingCapacity(), task.getLeft().getMaxPartials());

              for (int i = 1; i <= maxPartials; i++) {
                int threadNumber = Objects.requireNonNullElse(threadNumbers.poll(), 1);
                TaskCron4j taskCron4j =
                    new TaskCron4j(task.getLeft(), maxPartials, i, threadNumber);
                final TaskExecutor taskExecutor = scheduler.launch(taskCron4j);
                TaskStatus currentTask = new TaskStatusCron4j(taskCron4j, taskExecutor);

                addLogging(taskCron4j, currentTask, threadNumber);
                currentTask.onDone(
                    throwable -> {
                      threadNumbers.offer(threadNumber);
                      checkQueue();
                    });
                currentTasks.offer(currentTask);

                // TODO: currently not used?
                // task.getRight().complete(currentTask);
              }
            } else {
              int threadNumber = Objects.requireNonNullElse(threadNumbers.poll(), 1);
              TaskCron4j taskCron4j = new TaskCron4j(task.getLeft(), threadNumber);
              final TaskExecutor taskExecutor = scheduler.launch(taskCron4j);
              TaskStatus currentTask = new TaskStatusCron4j(taskCron4j, taskExecutor);

              addLogging(taskCron4j, currentTask, threadNumber);
              currentTask.onDone(
                  throwable -> {
                    threadNumbers.offer(threadNumber);
                    checkQueue();
                  });
              currentTasks.offer(currentTask);

              task.getRight().complete(currentTask);
            }
          }
        }
      }

      private synchronized void cleanup() {
        currentTasks.removeIf(TaskStatus::isDone);
      }
    };
  }

  private void addLogging(TaskCron4j taskCron4j, TaskStatus taskStatus, int threadNum) {
    String threadName = "bg-task-" + threadNum;
    Thread.currentThread().setName(threadName);

    Task task = taskCron4j.getTask();
    task.logContext();
    String partialSuffix =
        taskCron4j.isPartial()
            ? String.format(" [%d/%d]", taskCron4j.getPartial(), taskCron4j.getMaxPartials())
            : "";
    String part =
        taskCron4j.isPartial()
            ? String.format(" (part [%d/%d])", taskCron4j.getPartial(), taskCron4j.getMaxPartials())
            : "";
    LOGGER.info("{}{} started", task.getLabel(), part);

    taskStatus.onChange(
        (progress, message) -> {
          if (LOGGER.isDebugEnabled()) {
            Thread.currentThread().setName(threadName);
            LOGGER.debug(
                "{}: {} [{}]{}",
                task.getLabel(),
                message,
                new DecimalFormat("#%").format(progress),
                partialSuffix);
          }
        },
        1000);
    taskStatus.onDone(
        (optionalThrowable) -> {
          Thread.currentThread().setName(threadName);

          if (optionalThrowable.isPresent()) {
            LogContext.errorChain(
                LOGGER, optionalThrowable.get(), "{} failed{}", task.getLabel(), partialSuffix);
          } else {
            String time = pretty(taskStatus.getEndTime() - taskStatus.getStartTime());

            LOGGER.info("{}{} finished in {}", task.getLabel(), part, time);
          }
        });
  }

  private static String pretty(long milliseconds) {
    Duration d = Duration.ofSeconds(milliseconds / 1000);
    return AmountFormats.wordBased(d, Locale.ENGLISH);
  }
}
