/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain.resiliency;

import com.codahale.metrics.health.HealthCheck;
import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry.ChangeHandler;
import de.ii.xtraplatform.base.domain.util.Tuple;
import java.util.Optional;

@AutoMultiBind
public interface Volatile2 {
  enum State {
    UNAVAILABLE,
    LIMITED,
    AVAILABLE;

    public boolean isLowerThan(State other) {
      return this.ordinal() < other.ordinal();
    }
  }

  interface Polling {
    int getIntervalMs();

    Tuple<State, String> check();

    void poll();
  }

  default String getUniqueKey() {
    return String.format("%s/%s", this.getClass().getName(), getInstanceId().orElse("0"));
  }

  default Optional<String> getInstanceId() {
    return Optional.empty();
  }

  State getState();

  Optional<String> getMessage();

  Runnable onStateChange(ChangeHandler handler, boolean initialCall);

  default boolean isAvailable() {
    return getState() == State.AVAILABLE;
  }

  default boolean isUsable() {
    return isAvailable() || getState() == State.LIMITED;
  }

  default Optional<HealthCheck> asHealthCheck() {
    return Optional.of(HealthChecks.simple(this));
  }
}
