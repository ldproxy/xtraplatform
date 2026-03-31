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
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Singleton;
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
  public CompletionStage<Void> onStart(boolean isStartupAsync) {
    scheduler.start();

    return CompletableFuture.completedFuture(null);
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
    return new TaskQueueCron4j(
        scheduler,
        (taskCron4j, taskStatus) ->
            addLogging(taskCron4j, taskStatus, taskCron4j.getThreadNumber()),
        maxConcurrentTasks);
  }

  private void addLogging(TaskCron4j taskCron4j, TaskStatus taskStatus, int threadNum) {
    String threadName = "bg-task-" + threadNum;
    Thread.currentThread().setName(threadName);

    if (taskCron4j.getTask().isSilent()) {
      return;
    }

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
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("{}{} started", task.getLabel(), part);
    }

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

            if (LOGGER.isInfoEnabled()) {
              LOGGER.info("{}{} finished in {}", task.getLabel(), part, time);
            }
          }
        });
  }

  private static String pretty(long milliseconds) {
    Duration d = Duration.ofSeconds(milliseconds / 1000);
    return AmountFormats.wordBased(d, Locale.ENGLISH);
  }
}
