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
import de.ii.xtraplatform.base.domain.util.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class DelayedVolatile<T extends Volatile2> extends AbstractVolatile implements Volatile2 {

  private final boolean delegateHealth;
  private final List<Tuple<ChangeHandler, Boolean>> changeHandlers;
  private final List<Runnable> unwatchs;
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
    this.changeHandlers = new ArrayList<>();
    this.unwatchs = new ArrayList<>();
  }

  @Override
  public State getState() {
    return isPresent() ? get().getState() : State.UNAVAILABLE;
  }

  @Override
  public Optional<String> getMessage() {
    return isPresent() ? get().getMessage() : Optional.empty();
  }

  @Override
  public Runnable onStateChange(ChangeHandler handler, boolean initialCall) {
    return isPresent()
        ? get().onStateChange(handler, initialCall)
        : delayedOnStateChange(handler, initialCall);
  }

  private synchronized Runnable delayedOnStateChange(ChangeHandler handler, boolean initialCall) {
    changeHandlers.add(Tuple.of(handler, initialCall));
    int index = changeHandlers.size();

    return () -> {
      if (isPresent()) {
        unwatchs.get(index).run();
      } else {
        changeHandlers.remove(index);
      }
    };
  }

  @Override
  protected Set<String> getVolatileCapabilities() {
    return isPresent() && get() instanceof AbstractVolatile
        ? ((AbstractVolatile) get()).getVolatileCapabilities()
        : Set.of();
  }

  @Override
  public Optional<HealthCheck> asHealthCheck() {
    return delegateHealth && Objects.nonNull(dependency)
        ? dependency.asHealthCheck()
        : Optional.empty();
  }

  public synchronized void set(T volatile2) {
    /*if (Objects.nonNull(this.dependency)) {
      throw new IllegalStateException("DelayedVolatile already initialized");
    }*/

    this.dependency = volatile2;

    if (volatile2 instanceof AbstractVolatile) {
      ((AbstractVolatile) volatile2).onVolatileStart();
    }

    for (Tuple<ChangeHandler, Boolean> params : changeHandlers) {
      Runnable unwatch = volatile2.onStateChange(params.first(), params.second());
      unwatchs.add(unwatch);
    }
  }

  public synchronized void reset() {
    // this.dependency = null;
    changeHandlers.clear();
    unwatchs.forEach(Runnable::run);
    unwatchs.clear();
  }

  public boolean isPresent() {
    return Objects.nonNull(dependency);
  }

  public T get() {
    return dependency;
  }
}
