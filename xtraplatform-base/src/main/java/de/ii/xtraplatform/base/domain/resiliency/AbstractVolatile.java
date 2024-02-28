/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain.resiliency;

import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry.ChangeHandler;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractVolatile implements Volatile2 {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractVolatile.class);

  private final VolatileRegistry volatileRegistry;

  private final String uniqueKey;
  private final Set<String> capabilities;
  private State state;
  private String message;
  private boolean started;

  protected AbstractVolatile(VolatileRegistry volatileRegistry) {
    this(volatileRegistry, null);
  }

  protected AbstractVolatile(
      VolatileRegistry volatileRegistry, String uniqueKey, String... capabilities) {
    this.volatileRegistry = volatileRegistry;
    this.uniqueKey = uniqueKey;
    this.capabilities = Set.of(capabilities);
    this.state = State.UNAVAILABLE;
    this.message = null;
    this.started = false;
  }

  protected synchronized void onVolatileStart() {
    if (!started) {
      LOGGER.debug("START {}", getUniqueKey());
      volatileRegistry.register(this);
      this.started = true;
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
    return Optional.ofNullable(message);
  }

  @Override
  public Runnable onStateChange(ChangeHandler handler, boolean initialCall) {
    Runnable unwatch = volatileRegistry.watch(this, handler);

    if (initialCall && getState() != State.UNAVAILABLE) {
      handler.change(State.UNAVAILABLE, getState());
    }

    return unwatch;
  }

  protected final void setState(State state) {
    State from = this.state;
    this.state = state;

    volatileRegistry.change(this, from, state);
  }

  protected final void setMessage(String message) {
    this.message = message;
  }

  protected Set<String> getVolatileCapabilities() {
    return capabilities;
  }
}
