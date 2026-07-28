/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;
import java.time.Instant;
import java.util.OptionalInt;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableJobV2Impl.Builder.class)
public interface JobV2Impl extends JobV2 {

  NoArgGenerator UUID = Generators.defaultTimeBasedGenerator();

  @Override
  @Value.Default
  default String getId() {
    return UUID.generate().toString();
  }

  @Value.Default
  default int getPriority() {
    return 1000;
  }

  @Override
  String getType();

  @Override
  Object getDetails();

  OptionalInt getTimeout();

  OptionalInt getRetries();

  @Override
  @Value.Default
  default long getCreatedAt() {
    return Instant.now().getEpochSecond();
  }

  @Override
  @Value.Default
  default long getStartedAt() {
    return -1;
  }

  @Override
  @Value.Default
  default long getUpdatedAt() {
    return -1;
  }

  @Override
  @Value.Default
  default long getFinishedAt() {
    return -1;
  }

  @Override
  @Value.Default
  default Status getStatus() {
    return Status.ACCEPTED;
  }

  @Value.Default
  default int getTotal() {
    return 0;
  }

  @Value.Default
  default int getCurrent() {
    return 0;
  }

  @Override
  @Value.Derived
  default int getProgress() {
    int total = getTotal();

    if (total == 0) {
      if (getStartedAt() <= 0) {
        return 0;
      }
      return 100;
    }

    int current = getCurrent();

    if (current >= total) {
      return 100;
    }

    return (int) (((float) Math.max(current, 0)) / total) * 100;
  }

  @Value.Derived
  default boolean isStarted() {
    return getStartedAt() > 0;
  }

  @Value.Derived
  default boolean isDone() {
    return isStarted() && getTotal() == getCurrent();
  }
}
