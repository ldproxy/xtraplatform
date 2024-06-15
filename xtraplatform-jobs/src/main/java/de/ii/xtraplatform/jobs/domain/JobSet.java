/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.immutables.value.Value;

@Value.Immutable
public interface JobSet extends BaseJob {

  static JobSet of(String type, String entity, String label, String description, Object details) {
    return new ImmutableJobSet.Builder()
        .type(type)
        .entity(entity)
        .label(label)
        .description(description)
        .details(details)
        .startedAt(new AtomicLong())
        .total(new AtomicInteger())
        .current(new AtomicInteger())
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

  default List<BaseJob> done(Job job) {
    if (getSetup().isPresent() && Objects.equals(job.getId(), getSetup().get().getId())) {
      return List.of();
    }

    if (getCleanup().isPresent() && Objects.equals(job.getId(), getCleanup().get().getId())) {
      return getFollowUps();
    }

    getCurrent().incrementAndGet();

    if (isDone()) {
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

  // TODO: progress wrapper?

  AtomicLong getStartedAt();

  AtomicInteger getTotal();

  AtomicInteger getCurrent();

  default int getPercent() {
    return Math.round(((float) Math.max(getCurrent().get(), 0) / getTotal().get()) * 100);
  }

  default boolean isDone() {
    return getTotal().get() == getCurrent().get();
  }
}
