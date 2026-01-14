/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.app;

import de.ii.xtraplatform.services.domain.TaskContext;
import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zahnen
 */
public class TaskCron4j extends Task {

  private final de.ii.xtraplatform.services.domain.Task task;
  private final int maxPartials;
  private final int partial;
  private final int threadNumber;
  private final String threadName;
  private final AtomicInteger activePartials;

  public TaskCron4j(de.ii.xtraplatform.services.domain.Task task, int threadNumber) {
    this(task, 1, 1, threadNumber, new AtomicInteger(1));
  }

  public TaskCron4j(
      de.ii.xtraplatform.services.domain.Task task,
      int maxPartials,
      int partial,
      int threadNumber,
      AtomicInteger activePartials) {
    super();
    this.task = task;
    this.maxPartials = maxPartials;
    this.partial = partial;
    this.threadNumber = threadNumber;
    this.threadName = "bg-task-" + threadNumber;
    this.activePartials = activePartials;
  }

  public de.ii.xtraplatform.services.domain.Task getTask() {
    return task;
  }

  public int getMaxPartials() {
    return maxPartials;
  }

  public int getPartial() {
    return partial;
  }

  public boolean isPartial() {
    return getMaxPartials() > 1;
  }

  public String getThreadName() {
    return threadName;
  }

  public int getThreadNumber() {
    return threadNumber;
  }

  @Override
  public void execute(TaskExecutionContext taskExecutionContext) {
    final TaskContext taskContext =
        new TaskContextCron4j(
            taskExecutionContext,
            maxPartials,
            partial,
            threadName,
            task.getLabel(),
            activePartials);
    task.run(taskContext);
  }

  @Override
  public boolean canBePaused() {
    return true;
  }

  @Override
  public boolean canBeStopped() {
    return true;
  }

  @Override
  public boolean supportsStatusTracking() {
    return true;
  }

  @Override
  public boolean supportsCompletenessTracking() {
    return true;
  }
}
