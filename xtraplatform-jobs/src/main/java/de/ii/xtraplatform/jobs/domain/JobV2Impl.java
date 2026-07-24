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
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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

  List<? extends JobV2Impl> getFollowUps();

  OptionalInt getTimeout();

  OptionalInt getRetries();

  /*@Value.Default
  default AtomicReference<List<String>> getErrors() {
    return new AtomicReference<>(List.of());
  }*/

  @Override
  @Value.Default
  default AtomicLong getCreatedAt() {
    return new AtomicLong(Instant.now().getEpochSecond());
  }

  @Override
  @Value.Default
  default AtomicLong getStartedAt() {
    return new AtomicLong(-1);
  }

  @Override
  @Value.Default
  default AtomicLong getUpdatedAt() {
    return new AtomicLong(-1);
  }

  @Override
  @Value.Default
  default AtomicLong getFinishedAt() {
    return new AtomicLong(-1);
  }

  @Override
  @Value.Default
  default Status getStatus() {
    return Status.ACCEPTED;
  }

  @Value.Default
  default AtomicInteger getTotal() {
    return new AtomicInteger(0);
  }

  @Value.Default
  default AtomicInteger getCurrent() {
    return new AtomicInteger(0);
  }

  @Override
  default int getProgress() {
    int total = getTotal().get();

    if (total == 0) {
      if (getStartedAt().get() <= 0) {
        return 0;
      }
      return 100;
    }

    int current = getCurrent().get();

    if (current >= total) {
      return 100;
    }

    return (int) (((float) Math.max(current, 0)) / total) * 100;
  }

  default boolean isStarted() {
    return getStartedAt().get() > 0;
  }

  default boolean isDone() {
    return isStarted() && getTotal().get() == getCurrent().get();
  }

  default void init(int delta) {
    getTotal().addAndGet(delta);
  }

  default void update(int delta) {
    getCurrent().addAndGet(delta);
    getUpdatedAt().set(Instant.now().getEpochSecond());
  }
}
