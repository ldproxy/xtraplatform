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
import java.util.Objects;
import java.util.Optional;

/** @author zahnen */
public class TaskCron4j extends Task {

  private final de.ii.xtraplatform.services.domain.Task task;
  private final int maxPartials;
  private final int partial;
  private final String threadName;

  public TaskCron4j(de.ii.xtraplatform.services.domain.Task task, int threadNumber) {
    this(task, 1, 1, threadNumber);
  }

  public TaskCron4j(de.ii.xtraplatform.services.domain.Task task, int maxPartials, int partial, int threadNumber) {
    this.task = task;
    this.maxPartials = maxPartials;
    this.partial = partial;
    this.threadName = "bg-task-" + threadNumber;
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

  @Override
  public void execute(TaskExecutionContext taskExecutionContext) throws RuntimeException {
    final TaskContext taskContext = new TaskContextCron4j(taskExecutionContext, maxPartials, partial, threadName);
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
