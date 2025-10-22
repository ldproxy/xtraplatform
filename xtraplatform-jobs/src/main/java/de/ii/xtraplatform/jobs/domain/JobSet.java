/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.domain;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.immutables.value.Value;

@Value.Immutable
public interface JobSet extends BaseJob {

  interface JobSetDetails {
    void update(Map<String, String> parameters);

    void reset(Job job);

    String getLabel();
  }

  @Override
  JobSetDetails getDetails();

  static JobSet of(
      String type,
      int priority,
      String entity,
      String label,
      String description,
      JobSetDetails details) {
    return new ImmutableJobSet.Builder()
        .type(type)
        .priority(priority)
        .entity(entity)
        .label(label)
        .description(description)
        .details(details)
        .startedAt(new AtomicLong())
        .total(new AtomicInteger(0))
        .current(new AtomicInteger(0))
        .build();
  }

  default JobSet with(Job setup, Job cleanup) {
    return new ImmutableJobSet.Builder()
        .from(this)
        .setup(setup.with(this.getId()))
        .cleanup(cleanup.with(this.getId()))
        .build();
  }

  default JobSet with(BaseJob... followUps) {
    return new ImmutableJobSet.Builder().from(this).addFollowUps(followUps).build();
  }

  default JobSet with(String description, JobSetDetails details) {
    return new ImmutableJobSet.Builder()
        .from(this)
        .description(description)
        .details(details)
        .build();
  }

  default void start() {
    getStartedAt().set(Instant.now().getEpochSecond());
  }

  default List<BaseJob> done(Job job) {
    if (getSetup().isPresent() && Objects.equals(job.getId(), getSetup().get().getId())) {
      return List.of();
    }

    if (getCleanup().isPresent() && Objects.equals(job.getId(), getCleanup().get().getId())) {
      return getFollowUps();
    }

    getUpdatedAt().set(Instant.now().getEpochSecond());

    List<String> jobErrors = job.getErrors().get();
    if (!jobErrors.isEmpty()) {
      getErrors()
          .getAndUpdate(
              existingErrors -> {
                List<String> combined = new java.util.ArrayList<>(existingErrors);
                combined.addAll(jobErrors);
                return ImmutableList.copyOf(combined);
              });
    }

    if (isDone() && getFinishedAt().get() == -1) {
      getFinishedAt().set(Instant.now().getEpochSecond());
      return getCleanup().isPresent() ? List.of(getCleanup().get()) : getFollowUps();
    }

    return List.of();
  }

  @Value.Default
  default String getLabel() {
    return getType();
  }

  Optional<String> getDescription();

  Optional<String> getEntity();

  Optional<Job> getSetup();

  Optional<Job> getCleanup();
}
