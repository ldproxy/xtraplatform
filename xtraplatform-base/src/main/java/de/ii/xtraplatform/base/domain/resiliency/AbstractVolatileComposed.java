/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain.resiliency;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry.ChangeHandler;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractVolatileComposed extends AbstractVolatile
    implements VolatileComposed {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractVolatileComposed.class);

  private final Map<String, Volatile2> components;
  private final Map<String, AbstractVolatile> capabilities;
  private State baseState;

  public AbstractVolatileComposed(VolatileRegistry volatileRegistry, String... capabilities) {
    super(volatileRegistry);

    this.components = new LinkedHashMap<>();
    this.capabilities =
        Arrays.stream(capabilities)
            .map(
                capability ->
                    Map.entry(capability, new AbstractVolatile(volatileRegistry, capability) {}))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    this.baseState = State.UNAVAILABLE;
  }

  @Override
  protected void onVolatileStart() {
    this.baseState = State.UNAVAILABLE;
    super.onVolatileStart();
    for (Volatile2 v : components.values()) {
      if (v instanceof AbstractVolatile) {
        LOGGER.debug("START {}", v.getUniqueKey());
        ((AbstractVolatile) v).onVolatileStart();
        v.onStateChange(this::onChange, false);
      }
    }
    checkStates();
  }

  protected final void addSubcomponents(Volatile2... subs) {
    for (Volatile2 v : subs) {
      this.components.put(v.getUniqueKey(), v);

      if (v instanceof AbstractVolatile) {
        LOGGER.debug("START {}", v.getUniqueKey());
        ((AbstractVolatile) v).onVolatileStart();
      }
      // TODO: if started
      v.onStateChange(this::onChange, false);
    }
    checkStates();
  }

  protected void onVolatileStarted() {
    this.baseState = State.AVAILABLE;
    checkStates();
  }

  @Override
  public Set<String> getVolatileCapabilities() {
    return capabilities.keySet();
  }

  @Override
  public State getState(String capability) {
    return capabilities.get(capability).getState();
  }

  @Override
  public Optional<String> getMessage(String capability) {
    return capabilities.get(capability).getMessage();
  }

  @Override
  public Runnable onStateChange(String capability, ChangeHandler handler, boolean initialCall) {
    // TODO
    return capabilities.get(capability).onStateChange(handler, initialCall);
  }

  protected final Set<String> getComponents() {
    return components.keySet();
  }

  protected final Volatile2 getComponent(String subKey) {
    return components.get(subKey);
  }

  protected State getComposedState(State lowest, State highest) {
    if (lowest == State.AVAILABLE) {
      return State.AVAILABLE;
    } else if (State.UNAVAILABLE.isLowerThan(highest)) {
      return State.LIMITED;
    }
    return State.UNAVAILABLE;
  }

  private void onChange(State from, State to) {
    /*if (to.isLowerThan(getState())) {
      setState(to);
      // TODO: messages
    }*/
    checkStates();
  }

  private void checkStates() {
    State lowestState = baseState;
    State highestState = State.UNAVAILABLE;

    State newState = reconcile(lowestState, highestState);

    for (Map.Entry<String, AbstractVolatile> entry : capabilities.entrySet()) {
      String capability = entry.getKey();
      AbstractVolatile vol = entry.getValue();

      State newState2 = reconcile(lowestState, highestState, capability);

      if (newState2 != vol.getState()) {
        vol.setState(newState2);
      }

      if (newState2.isLowerThan(newState)) {
        newState = newState2;
      }
    }

    if (newState != getState()) {
      setState(newState);
    }
  }

  private State reconcile(State lowestState, State highestState) {
    for (Volatile2 dep : components.values()) {
      if (dep.getState().isLowerThan(lowestState)) {
        lowestState = dep.getState();
      }
      if (highestState.isLowerThan(dep.getState())) {
        highestState = dep.getState();
      }
    }

    return getComposedState(lowestState, highestState);
  }

  private State reconcile(State lowestState, State highestState, String capability) {
    boolean atLeastOne = false;

    for (Volatile2 dep : components.values()) {
      if (dep instanceof AbstractVolatile
          && ((AbstractVolatile) dep).getVolatileCapabilities().contains(capability)) {
        atLeastOne = true;

        if (dep.getState().isLowerThan(lowestState)) {
          lowestState = dep.getState();
        }
        if (highestState.isLowerThan(dep.getState())) {
          highestState = dep.getState();
        }
      }
    }

    if (!atLeastOne) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.error(
            "No component with capability '{}' found for volatile: {}", capability, getUniqueKey());
      }
      return State.UNAVAILABLE;
    }

    return getComposedState(lowestState, highestState);
  }
}
