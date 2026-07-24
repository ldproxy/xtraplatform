/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.domain;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface JobQueueV2 {
  JobV2 createJob(String type, Map<String, Object> inputs);

  JobV2 createJob(String type, Map<String, Object> inputs, Object details);

  void push(JobV2 job);

  void push(JobV2 job, Consumer<JobV2> onChange);

  CompletableFuture<JobV2> pushSync(JobV2 job);

  CompletableFuture<JobV2> pushSync(JobV2 job, Consumer<JobV2> onChange);

  JobV2 get(String jobId);
}
