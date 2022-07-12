/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.app;

import de.ii.xtraplatform.services.domain.TaskStatus;
import it.sauronsoftware.cron4j.TaskExecutor;
import it.sauronsoftware.cron4j.TaskExecutorListener;
import java.time.Instant;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
public class TaskStatusCron4j implements TaskStatus {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskStatusCron4j.class);

  private final String id;
  private final String label;
  private final String threadName;
  private long endTime;
  private final TaskExecutor taskExecutor;

  public TaskStatusCron4j(TaskCron4j taskCron4j, TaskExecutor taskExecutor) {
    this.id = taskCron4j.getTask().getId();
    this.label = taskCron4j.getTask().getLabel();
    this.threadName = taskCron4j.getThreadName();
    this.taskExecutor = taskExecutor;
    this.endTime = 0;

    taskExecutor.addTaskExecutorListener(
        new TaskExecutorListener() {
          @Override
          public void executionPausing(TaskExecutor taskExecutor) {}

          @Override
          public void executionResuming(TaskExecutor taskExecutor) {}

          @Override
          public void executionStopping(TaskExecutor taskExecutor) {}

          @Override
          public void executionTerminated(TaskExecutor taskExecutor, Throwable throwable) {
            endTime = Instant.now().toEpochMilli();
          }

          @Override
          public void statusMessageChanged(TaskExecutor taskExecutor, String s) {}

          @Override
          public void completenessValueChanged(TaskExecutor taskExecutor, double v) {}
        });
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getLabel() {
    return label;
  }

  @Override
  public String getThreadName() {
    return threadName;
  }

  @Override
  public String getStatusMessage() {
    return taskExecutor.getStatusMessage();
  }

  @Override
  public double getProgress() {
    return taskExecutor.getCompleteness();
  }

  @Override
  public long getStartTime() {
    return taskExecutor.getStartTime();
  }

  @Override
  public long getEndTime() {
    return endTime;
  }

  @Override
  public boolean isDone() {
    return taskExecutor.getStartTime() >= 0 && endTime > 0;
  }

  @Override
  public void onDone(Consumer<Optional<Throwable>> runnable) {
    taskExecutor.addTaskExecutorListener(
        new TaskExecutorListener() {
          @Override
          public void executionPausing(TaskExecutor taskExecutor) {}

          @Override
          public void executionResuming(TaskExecutor taskExecutor) {}

          @Override
          public void executionStopping(TaskExecutor taskExecutor) {}

          @Override
          public void executionTerminated(TaskExecutor taskExecutor, Throwable throwable) {
            runnable.accept(Optional.ofNullable(throwable));
          }

          @Override
          public void statusMessageChanged(TaskExecutor taskExecutor, String s) {}

          @Override
          public void completenessValueChanged(TaskExecutor taskExecutor, double v) {}
        });
  }

  @Override
  public void onChange(BiConsumer<Double, String> statusConsumer, long minInterval) {
    taskExecutor.addTaskExecutorListener(
        new TaskExecutorListener() {
          private long last = Instant.now().toEpochMilli();

          @Override
          public void executionPausing(TaskExecutor taskExecutor) {}

          @Override
          public void executionResuming(TaskExecutor taskExecutor) {}

          @Override
          public void executionStopping(TaskExecutor taskExecutor) {}

          @Override
          public void executionTerminated(TaskExecutor taskExecutor, Throwable throwable) {}

          @Override
          public void statusMessageChanged(TaskExecutor taskExecutor, String s) {
            synchronized (this) {
              long now = Instant.now().toEpochMilli();
              if (now - last > minInterval) {
                statusConsumer.accept(taskExecutor.getCompleteness(), s);
                last = now;
              }
            }
          }

          @Override
          public void completenessValueChanged(TaskExecutor taskExecutor, double v) {
            synchronized (this) {
              long now = Instant.now().toEpochMilli();
              if (now - last > minInterval) {
                statusConsumer.accept(v, taskExecutor.getStatusMessage());
                last = now;
              }
            }
          }
        });
  }

  @Override
  public void stop() {
    taskExecutor.stop();
  }
}
