/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import org.immutables.value.Value;

@Value.Immutable
public interface Job extends BaseJob {

  static Job of(String type, Object details) {
    return new ImmutableJob.Builder().type(type).details(details).build();
  }

  static Job of(String type, Object details, String partOf) {
    return new ImmutableJob.Builder().type(type).details(details).partOf(partOf).build();
  }

  default Job started(String executor) {
    return new ImmutableJob.Builder()
        .from(this)
        .executor(executor)
        .startedAt(Instant.now().getEpochSecond())
        .build();
  }

  default Job with(String jobSetId) {
    return new ImmutableJob.Builder().from(this).partOf(jobSetId).build();
  }

  Optional<String> getExecutor();

  Optional<String> getPartOf();

  OptionalLong getStartedAt();
}
