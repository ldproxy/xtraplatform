/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.domain;

import de.ii.xtraplatform.base.domain.resiliency.VolatileComposed;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public interface JobQueue extends JobQueueMin, VolatileComposed {

  void push(BaseJob job, boolean untake);

  @Override
  default void push(BaseJob job) {
    push(job, false);
  }

  void onPush(Consumer<String> callback);

  Optional<Job> take(String type, String executor);

  boolean done(String jobId);

  boolean doneSet(String jobSetId);

  boolean error(String jobId, String error, boolean retry);

  Collection<JobSet> getSets();

  Map<String, Map<Integer, List<Job>>> getOpen();

  Collection<Job> getTaken();

  Collection<Job> getFailed();

  JobSet getSet(String setId);

  void setJobTypes(Function<String, Optional<? extends Class<?>>> jobTypesMapper);
}
