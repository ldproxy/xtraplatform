/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.app;

import de.ii.xtraplatform.services.domain.TaskContext;
import it.sauronsoftware.cron4j.TaskExecutionContext;

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
