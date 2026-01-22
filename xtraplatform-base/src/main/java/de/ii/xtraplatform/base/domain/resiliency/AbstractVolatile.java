/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain.resiliency;

import com.codahale.metrics.health.HealthCheck;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry.ChangeHandler;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public abstract class AbstractVolatile implements Volatile2, VolatileRegistered {

  protected final VolatileRegistry volatileRegistry;
  private final boolean noHealth;

  private final String uniqueKey;
  private final Set<String> capabilities;
  private State state;
  private boolean started;
  private Optional<String> message = Optional.empty();

  protected AbstractVolatile(VolatileRegistry volatileRegistry) {
    this(volatileRegistry, null);
  }

  protected AbstractVolatile(VolatileRegistry volatileRegistry, boolean noHealth) {
    this(volatileRegistry, noHealth, null);
  }

  protected AbstractVolatile(
      VolatileRegistry volatileRegistry, String uniqueKey, String... capabilities) {
    this(volatileRegistry, false, uniqueKey, capabilities);
  }

  protected AbstractVolatile(
      VolatileRegistry volatileRegistry,
      boolean noHealth,
      String uniqueKey,
      String... capabilities) {
    this.volatileRegistry = volatileRegistry;
    this.noHealth = noHealth;
    this.uniqueKey = uniqueKey;
    this.capabilities = Set.of(capabilities);
    this.state = State.UNAVAILABLE;
    this.message = Optional.empty();
    this.started = false;
  }

  @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
  protected synchronized void onVolatileStart() {
    if (!started) {
      volatileRegistry.register(this);
      this.started = true;
    }
  }

  @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
  protected synchronized void onVolatileStop() {
    if (started) {
      this.started = false;
      setState(State.UNAVAILABLE);
    }
  }

  protected final boolean isStarted() {
    return started;
  }

  @Override
  public String getUniqueKey() {
    return Objects.requireNonNullElseGet(uniqueKey, Volatile2.super::getUniqueKey);
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
    Runnable unwatch = volatileRegistry.watch(this, handler);

    if (initialCall && getState() != State.UNAVAILABLE) {
      handler.change(State.UNAVAILABLE, getState());
    }

    return unwatch;
  }

  @Override
  public Optional<HealthCheck> asHealthCheck() {
    return noHealth ? Optional.empty() : Volatile2.super.asHealthCheck();
  }

  @Override
  public VolatileRegistry getVolatileRegistry() {
    return volatileRegistry;
  }

  protected final void setState(State state) {
    if (state != this.state) {
      State from = this.state;
      this.state = state;

      volatileRegistry.change(this, from, state);
    }
  }

  protected final void setMessage(String message) {
    this.message = Optional.ofNullable(message);
  }

  protected Set<String> getVolatileCapabilities() {
    return capabilities;
  }
}
