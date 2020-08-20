/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.scheduler.cron4j;

import com.google.common.base.Strings;
import de.ii.xtraplatform.scheduler.api.TaskContext;
import it.sauronsoftware.cron4j.TaskExecutionContext;

import java.util.Objects;

/**
 * @author zahnen
 */
public class TaskContextCron4j implements TaskContext {
    private final TaskExecutionContext taskExecutionContext;

    public TaskContextCron4j(TaskExecutionContext taskExecutionContext) {
        this.taskExecutionContext = taskExecutionContext;
    }

    @Override
    public void setStatusMessage(String statusMessage) {
        taskExecutionContext.setStatusMessage(statusMessage);
    }

    @Override
    public void setCompleteness(double completeness) {
        taskExecutionContext.setCompleteness(completeness);
    }

    @Override
    public void pauseIfRequested() {
        taskExecutionContext.pauseIfRequested();
    }

    @Override
    public boolean isStopped() {
        return taskExecutionContext.isStopped();
    }
}
