/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.redis.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xtraplatform.jobs.domain.Job;
import de.ii.xtraplatform.jobs.domain.Job.JobDetails;
import de.ii.xtraplatform.jobs.domain.JobSet;
import de.ii.xtraplatform.jobs.domain.JobSet.JobSetDetails;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class JobDetailsMapper {
  private final ObjectMapper mapper;
  private Function<String, Optional<? extends Class<?>>> jobTypes;

  @Inject
  JobDetailsMapper(ObjectMapper mapper) {
    this.mapper = mapper;
    this.jobTypes = type -> Optional.empty();
  }

  public void setJobTypes(Function<String, Optional<? extends Class<?>>> jobTypesMapper) {
    this.jobTypes = jobTypesMapper;
  }

  public <T> T getJobDetails(Class<T> detailsType, Job job) {
    return detailsType.cast(unpackDetails(job));
  }

  public <T> T getJobSetDetails(Class<T> detailsType, JobSet jobSet) {
    return detailsType.cast(unpackSetDetails(jobSet));
  }

  private Object unpackDetails(Job job) {
    if (Objects.nonNull(job)
        && Objects.nonNull(job.getDetails())
        && job.getDetails() instanceof Map
        && !((Map<?, ?>) job.getDetails()).isEmpty()) {
      try {
        Object details =
            mapper.readValue(
                mapper.writeValueAsBytes(job.getDetails()),
                jobTypes.apply(job.getType()).orElseThrow());
        if (details instanceof JobDetails) {
          return details;
        }
      } catch (IOException e) {
        throw new IllegalStateException("Failed to deserialize job details", e);
      }
    }
    return job.getDetails();
  }

  private Object unpackSetDetails(JobSet jobSet) {
    if (Objects.nonNull(jobSet)
        && Objects.nonNull(jobSet.getDetails())
        && jobSet.getDetails() instanceof Map
        && !((Map<?, ?>) jobSet.getDetails()).isEmpty()) {
      try {
        Object details =
            mapper.readValue(
                mapper.writeValueAsBytes(jobSet.getDetails()),
                jobTypes.apply(jobSet.getType()).orElseThrow());
        if (details instanceof JobSetDetails) {
          return details;
        }
      } catch (IOException e) {
        throw new IllegalStateException("Failed to deserialize job set details", e);
      }
    }
    return jobSet.getDetails();
  }
}
