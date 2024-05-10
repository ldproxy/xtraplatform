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

public class DelayedVolatile<T extends Volatile2> extends AbstractVolatileComposed
    implements Volatile2 {

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
    super(uniqueKey, volatileRegistry, false, capabilities);

    this.delegateHealth = delegateHealth;
  }

  @Override
  public State getState() {
    return isPresent() ? super.getState() : State.UNAVAILABLE;
  }

  @Override
  public Optional<String> getMessage() {
    return isPresent() ? super.getMessage() : Optional.empty();
  }

  @Override
  public Optional<HealthCheck> asHealthCheck() {
    return delegateHealth && Objects.nonNull(dependency)
        ? dependency.asHealthCheck()
        : Optional.empty();
  }

  public synchronized void set(T volatile2) {
    addSubcomponent("delayed", volatile2, false, getVolatileCapabilities().toArray(new String[0]));

    this.dependency = volatile2;

    onVolatileStarted();
  }

  public boolean isPresent() {
    return Objects.nonNull(dependency);
  }

  public T get() {
    return dependency;
  }
}
