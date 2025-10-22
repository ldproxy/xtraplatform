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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.immutables.value.Value;

@Value.Immutable
public interface Job extends BaseJob {

  static Job of(String type, int priority, Object details) {
    return new ImmutableJob.Builder().type(type).priority(priority).details(details).build();
  }

  static Job of(String type, int priority, Object details, String partOf, int total) {
    return new ImmutableJob.Builder()
        .type(type)
        .priority(priority)
        .details(details)
        .partOf(partOf)
        .total(new AtomicInteger(total))
        .build();
  }

  default Job started(String executor) {
    return new ImmutableJob.Builder()
        .from(this)
        .executor(executor)
        .startedAt(new AtomicLong(Instant.now().getEpochSecond()))
        .updatedAt(new AtomicLong(Instant.now().getEpochSecond()))
        .build();
  }

  default Job reset() {
    return new ImmutableJob.Builder()
        .from(this)
        .executor(Optional.empty())
        .startedAt(new AtomicLong(-1))
        .updatedAt(new AtomicLong(-1))
        .current(new AtomicInteger(0))
        .build();
  }

  default Job retry(String error) {
    List<String> errors = new ArrayList<>(getErrors().get());
    errors.add(error);

    return new ImmutableJob.Builder()
        .from(this)
        .retries(this.getRetries().orElse(0) + 1)
        .errors(new AtomicReference<>(ImmutableList.copyOf(errors)))
        .build();
  }

  default Job failed(String error) {
    List<String> errors = new ArrayList<>(getErrors().get());
    errors.add(error);

    return new ImmutableJob.Builder()
        .from(this)
        .errors(new AtomicReference<>(ImmutableList.copyOf(errors)))
        .build();
  }

  default Job with(String jobSetId) {
    return new ImmutableJob.Builder().from(this).partOf(jobSetId).build();
  }

  default Job with(BaseJob... followUps) {
    return new ImmutableJob.Builder().from(this).addFollowUps(followUps).build();
  }

  default Job with(Collection<? extends BaseJob> followUps) {
    return new ImmutableJob.Builder().from(this).addAllFollowUps(followUps).build();
  }

  Optional<String> getExecutor();

  Optional<String> getPartOf();
}
