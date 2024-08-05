/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.domain;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;
import java.time.Instant;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.immutables.value.Value;

public interface BaseJob {

  NoArgGenerator UUID = Generators.defaultTimeBasedGenerator();

  @Value.Default
  default String getId() {
    return UUID.generate().toString();
  }

  String getType();

  Object getDetails();

  List<BaseJob> getFollowUps();

  List<String> getErrors();

  OptionalInt getTimeout();

  OptionalInt getRetries();

  // TODO: progress wrapper?

  @Value.Default
  default AtomicLong getStartedAt() {
    return new AtomicLong(-1);
  }

  @Value.Default
  default AtomicLong getUpdatedAt() {
    return new AtomicLong(-1);
  }

  @Value.Default
  default AtomicInteger getTotal() {
    return new AtomicInteger(0);
  }

  @Value.Default
  default AtomicInteger getCurrent() {
    return new AtomicInteger(0);
  }

  default int getPercent() {
    int total = getTotal().get();

    if (total == 0) {
      if (getStartedAt().get() <= 0) {
        return 0;
      }
      return 100;
    }

    if (total == -1) {
      return 0;
    }

    int current = getCurrent().get();

    if (current >= total) {
      return 100;
    }

    return (int) ((((float) Math.max(current, 0)) / total) * 100);
  }

  default boolean isDone() {
    return getTotal().get() == getCurrent().get();
  }

  default void init(int delta) {
    getTotal().addAndGet(delta);
  }

  default void update(int delta) {
    getCurrent().addAndGet(delta);
    getUpdatedAt().set(Instant.now().getEpochSecond());
  }
}
