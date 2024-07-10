/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.domain;

import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;

public interface JobQueue {

  void push(BaseJob job, boolean untake);

  default void push(BaseJob job) {
    push(job, false);
  }

  Optional<Job> take(String type, String executor);

  boolean done(String jobId);

  boolean error(String jobId, String error, boolean retry);

  Collection<JobSet> getSets();

  Map<String, Deque<Job>> getOpen();

  Collection<Job> getTaken();

  JobSet getSet(String setId);
}
