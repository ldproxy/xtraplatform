/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.domain;

import de.ii.xtraplatform.runtime.domain.LogContext;
import java.util.Optional;
import java.util.function.BiConsumer;

public interface ServiceBackgroundTask<T extends Service> {

  Class<T> getServiceType();

  String getLabel();

  void run(T service, TaskContext taskContext);

  default boolean runOnStart(T service) {
    return true;
  }

  default Optional<String> runPeriodic(T service) {
    return Optional.empty();
  }

  default String getQueue() {
    return ServiceBackgroundTasks.COMMON_QUEUE;
  }

  default Task getTask(T service, String label) {
    return new BoundTask<>(service, label, this::run);
  }

  class BoundTask<T extends Service> implements Task {

    private final T service;
    private final String label;
    private final BiConsumer<T, TaskContext> runnable;

    public BoundTask(T service, String label, BiConsumer<T, TaskContext> runnable) {
      this.service = service;
      this.label = label;
      this.runnable = runnable;
    }

    @Override
    public String getId() {
      return service.getId();
    }

    @Override
    public String getLabel() {
      return label;
    }

    @Override
    public void run(TaskContext taskContext) {
      Runnable taskWithMdc =
          () -> {
            Thread.currentThread().setName("bg-task-1");
            logContext();
            runnable.accept(service, taskContext);
          };
      taskWithMdc.run();
    }

    @Override
    public void logContext() {
      LogContext.put(LogContext.CONTEXT.SERVICE, service.getId());
    }
  }
}
