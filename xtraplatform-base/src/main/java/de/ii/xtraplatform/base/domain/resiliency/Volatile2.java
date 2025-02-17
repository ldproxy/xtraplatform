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

  default Optional<HealthInfo> getHealthInfo() {
    return Optional.empty();
  }

  default boolean isAvailable() {
    return getState() == State.AVAILABLE;
  }

  default boolean isUsable() {
    return isAvailable() || getState() == State.LIMITED;
  }

  default Optional<HealthCheck> asHealthCheck() {
    return Optional.of(HealthChecks.simple(this));
  }

  static Volatile2 available(String uniqueKey) {
    return fixed(uniqueKey, State.AVAILABLE, Optional.empty());
  }

  static Volatile2 limited(String uniqueKey, Optional<String> message) {
    return fixed(uniqueKey, State.LIMITED, message);
  }

  static Volatile2 unavailable(String uniqueKey, Optional<String> message) {
    return fixed(uniqueKey, State.UNAVAILABLE, message);
  }

  static Volatile2 fixed(String uniqueKey, State state, Optional<String> message) {
    return new VolatileFixed(uniqueKey, state, message);
  }

  class VolatileFixed implements Volatile2 {

    private final String uniqueKey;
    private final State state;
    private final Optional<String> message;

    public VolatileFixed(String uniqueKey, State state, Optional<String> message) {
      this.uniqueKey = uniqueKey;
      this.state = state;
      this.message = message;
    }

    @Override
    public String getUniqueKey() {
      return uniqueKey;
    }

    @Override
    public State getState() {
      return state;
    }

    @Override
    public Optional<String> getMessage() {
      return message;
    }

    @Override
    public Runnable onStateChange(ChangeHandler handler, boolean initialCall) {
      return () -> {};
    }
  }

  // TODO: records are not yet working with the docs plugin
  // record HealthInfo(String label, String description, boolean hidden) {}
  class HealthInfo {
    private final String label;
    private final String description;
    private final boolean hidden;

    public HealthInfo(String label, String description, boolean hidden) {
      this.label = label;
      this.description = description;
      this.hidden = hidden;
    }

    public String label() {
      return label;
    }

    public String description() {
      return description;
    }

    public boolean hidden() {
      return hidden;
    }
  }
}
