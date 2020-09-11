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

/** @author zahnen */
public class TaskStatusCron4j implements TaskStatus {

  private final String id;
  private final String label;
  private long endTime;
  private final TaskExecutor taskExecutor;

  public TaskStatusCron4j(String id, String label, TaskExecutor taskExecutor) {
    this.id = id;
    this.label = label;
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
    return taskExecutor.getStartTime() >= 0 && !taskExecutor.isAlive();
  }

  @Override
  public void onDone(Runnable runnable) {
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
            runnable.run();
          }

          @Override
          public void statusMessageChanged(TaskExecutor taskExecutor, String s) {}

          @Override
          public void completenessValueChanged(TaskExecutor taskExecutor, double v) {}
        });
  }
}
