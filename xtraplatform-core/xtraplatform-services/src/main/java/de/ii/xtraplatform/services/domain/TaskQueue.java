/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.domain;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author zahnen
 */
public interface TaskQueue {
  CompletableFuture<TaskStatus> launch(Task task);

  CompletableFuture<TaskStatus> launch(Task task, long delay);

  void remove(Task task);

  List<Task> getFutureTasks();

  Optional<TaskStatus> getCurrentTask();
}
