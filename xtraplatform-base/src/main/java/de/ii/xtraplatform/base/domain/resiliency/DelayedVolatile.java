/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain.resiliency;

import com.codahale.metrics.health.HealthCheck;
import java.util.Objects;
import java.util.Optional;

public class DelayedVolatile<T extends Volatile2> extends AbstractVolatile implements Volatile2 {

  private final boolean delegateHealth;
  private T dependency;

  public DelayedVolatile(
      VolatileRegistry volatileRegistry, String uniqueKey, String... capabilities) {
    this(volatileRegistry, uniqueKey, true, capabilities);
  }

  public DelayedVolatile(
      VolatileRegistry volatileRegistry,
      String uniqueKey,
      boolean delegateHealth,
      String... capabilities) {
    super(volatileRegistry, uniqueKey, capabilities);

    this.delegateHealth = delegateHealth;
  }

  @Override
  public Optional<HealthCheck> asHealthCheck() {
    return delegateHealth && Objects.nonNull(dependency)
        ? dependency.asHealthCheck()
        : Optional.empty();
  }

  public void set(T volatile2) {
    if (Objects.nonNull(this.dependency)) {
      throw new IllegalStateException("DelayedVolatile already initialized");
    }

    this.dependency = volatile2;

    checkStates();

    if (volatile2 instanceof AbstractVolatile) {
      ((AbstractVolatile) volatile2).onVolatileStart();
    }

    volatile2.onStateChange((from, to) -> checkStates(), true);
  }

  public boolean isPresent() {
    return Objects.nonNull(dependency);
  }

  public T get() {
    return dependency;
  }

  private void checkStates() {
    State lowestState = Objects.isNull(dependency) ? State.UNAVAILABLE : State.AVAILABLE;

    if (Objects.nonNull(dependency) && dependency.getState().isLowerThan(lowestState)) {
      lowestState = dependency.getState();
    }

    if (lowestState != getState()) {
      setState(lowestState);
    }
  }
}
