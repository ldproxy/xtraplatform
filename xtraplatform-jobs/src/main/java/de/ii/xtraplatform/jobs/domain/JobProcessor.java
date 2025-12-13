/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import java.util.Map;
import java.util.Objects;

@AutoMultiBind
public interface JobProcessor<T, U> {

  String getJobType();

  int getPriority();

  int getConcurrency(JobSet jobSet);

  JobResult process(Job job, JobSet jobSet, JobQueueMin jobQueue);

  Class<T> getDetailsType();

  Class<U> getSetDetailsType();

  default boolean canHandle(Job job) {
    return Objects.equals(job.getType(), getJobType())
        && (Objects.isNull(job.getDetails()) || getDetailsType().isInstance(job.getDetails()));
  }

  default T getDetails(Job job, JobQueueMin jobQueue) {
    return jobQueue.getJobDetails(getDetailsType(), job);
  }

  default U getSetDetails(JobSet jobSet, JobQueueMin jobQueue) {
    return jobQueue.getJobSetDetails(getSetDetailsType(), jobSet);
  }

  default Map<String, Class<?>> getJobTypes() {
    return Map.of();
  }
}
