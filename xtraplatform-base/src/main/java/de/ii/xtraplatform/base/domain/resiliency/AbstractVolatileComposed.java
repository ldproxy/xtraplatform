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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractVolatileComposed extends AbstractVolatile
    implements VolatileComposed {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractVolatileComposed.class);

  private final Map<String, Volatile2> initComponents;
  private final Map<String, Volatile2> components;
  private final Map<String, Set<String>> componentCapabilities;
  private final Map<String, AbstractVolatile> capabilities;
  private final boolean noHealth;
  private State baseState;
  private boolean ready;
  private boolean initialized;

  public AbstractVolatileComposed(VolatileRegistry volatileRegistry, String... capabilities) {
    this(null, volatileRegistry, false, capabilities);
  }

  public AbstractVolatileComposed(
      VolatileRegistry volatileRegistry, boolean noHealth, String... capabilities) {
    this(null, volatileRegistry, noHealth, capabilities);
  }

  public AbstractVolatileComposed(
      String uniqueId,
      VolatileRegistry volatileRegistry,
      boolean noHealth,
      String... capabilities) {
    super(volatileRegistry, uniqueId);

    this.initComponents = new LinkedHashMap<>();
    this.components = new LinkedHashMap<>();
    this.componentCapabilities = new LinkedHashMap<>();
    this.capabilities = new LinkedHashMap<>();
    this.noHealth = noHealth;
    this.baseState = State.UNAVAILABLE;

    Arrays.stream(capabilities)
        .forEach(
            capability ->
                this.capabilities.putIfAbsent(
                    capability, new AbstractVolatile(volatileRegistry, capability) {}));
  }

  @Override
  protected void onVolatileStart() {
    if (!isStarted()) {
      this.baseState = State.UNAVAILABLE;
      super.onVolatileStart();
      for (Volatile2 v : components.values()) {
        if (v instanceof AbstractVolatile) {
          ((AbstractVolatile) v).onVolatileStart();
          v.onStateChange(this::onChange, false);
        }
      }
      checkStates();
    }
  }

  protected Tuple<State, String> volatileInit() {
    // LOGGER.debug("INIT {}", getUniqueKey());
    return Tuple.of(State.AVAILABLE, null);
  }

  protected void onInitComponentsAvailable() {
    this.initialized = true;
    Tuple<State, String> result = volatileInit();

    if (Objects.nonNull(result.second())) {
      setMessage(result.second());
    }

    this.baseState = result.first();
    checkStates();
  }

  protected void onVolatileStarted() {
    this.ready = true;
    checkStates();

    /*volatileRegistry
    .onAvailable(components.values())
    .thenRun(
        () -> {
          Tuple<State, String> result = volatileInit();

          if (Objects.nonNull(result.second())) {
            setMessage(result.second());
          }

          this.baseState = result.first();
          checkStates();
        });*/
  }

  @Override
  public Optional<HealthCheck> asHealthCheck() {
    return noHealth ? Optional.empty() : VolatileComposed.super.asHealthCheck();
  }

  protected final void addSubcomponent(Volatile2 v, String... capabilities) {
    addSubcomponent(v.getUniqueKey(), v, false, capabilities);
  }

  protected final void addSubcomponent(Volatile2 v, boolean neededForInit, String... capabilities) {
    addSubcomponent(v.getUniqueKey(), v, neededForInit, capabilities);
  }

  protected final void addSubcomponent(String localKey, Volatile2 v, String... capabilities) {
    addSubcomponent(localKey, v, false, capabilities);
  }

  protected final void addSubcomponent(
      String localKey, Volatile2 v, boolean neededForInit, String... capabilities) {
    this.components.put(localKey, v);
    this.componentCapabilities.put(
        localKey,
        Arrays.stream(capabilities)
            .filter(this.capabilities::containsKey)
            .collect(Collectors.toSet()));

    if (neededForInit) {
      this.initComponents.put(localKey, v);
    }

    if (v instanceof AbstractVolatile) {
      ((AbstractVolatile) v).onVolatileStart();
    }

    v.onStateChange(this::onChange, false);

    // checkStates();
  }

  protected final void addCapability(String capability, String label) {
    addCapability(capability, label, "", false);
  }

  protected final void addCapability(String capability, String label, String description) {
    addCapability(capability, label, description, false);
  }

  protected final void addHiddenCapability(String capability, String label) {
    addCapability(capability, label, "", true);
  }

  private void addCapability(String capability, String label, String description, boolean hidden) {
    if (!this.capabilities.containsKey(capability)) {
      AbstractVolatile vol = new AbstractVolatile(volatileRegistry, capability) {};
      vol.setHealthInfo(label, description, hidden);
      this.capabilities.put(capability, vol);
    }
  }

  protected final void addStaticCapability(String capability, String label) {
    addHiddenCapability(capability, label);

    addSubcomponent(Volatile2.available(capability), capability);
  }

  @Override
  public Set<String> getVolatileCapabilities() {
    return capabilities.keySet();
  }

  @Override
  public State getState(String capability) {
    return capabilities.containsKey(capability)
        ? capabilities.get(capability).getState()
        : State.UNAVAILABLE;
  }

  @Override
  public Optional<String> getMessage(String capability) {
    return capabilities.containsKey(capability)
        ? capabilities.get(capability).getMessage()
        : Optional.empty();
  }

  @Override
  public Optional<HealthInfo> getHealthInfo(String capability) {
    return capabilities.containsKey(capability)
        ? capabilities.get(capability).getHealthInfo()
        : Optional.empty();
  }

  @Override
  public Runnable onStateChange(String capability, ChangeHandler handler, boolean initialCall) {
    return capabilities.containsKey(capability)
        ? capabilities.get(capability).onStateChange(handler, initialCall)
        : () -> {};
  }

  protected final Set<String> getComponents() {
    return components.entrySet().stream()
        .filter(entry -> !(entry.getValue() instanceof VolatileFixed))
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  protected final Volatile2 getComponent(String subKey) {
    return components.get(subKey);
  }

  protected final boolean hasComponent(String subKey) {
    return components.containsKey(subKey);
  }

  protected final boolean hasCapability(String subKey) {
    return capabilities.containsKey(subKey);
  }

  protected final Set<String> getComponentCapabilities(String subKey) {
    return componentCapabilities.getOrDefault(subKey, Set.of());
  }

  private void onChange(State from, State to) {
    /*if (to.isLowerThan(getState())) {
      setState(to);
      // TODO: messages
    }*/
    checkStates();
  }

  private void checkStates() {
    if (!isStarted()) {
      return;
    }
    if (baseState != State.AVAILABLE) {
      if (ready
          && !initialized
          && initComponents.values().stream().allMatch(Volatile2::isAvailable)) {
        onInitComponentsAvailable();
      }
      return;
    }

    State newState =
        capabilities.isEmpty() ? reconcileStateComponents(null) : reconcileStateCapabilities();

    if (newState != getState()) {
      setState(newState);
    }
  }

  protected State reconcileStateCapabilities() {
    State composedState = State.AVAILABLE;

    for (Map.Entry<String, AbstractVolatile> entry : capabilities.entrySet()) {
      String capability = entry.getKey();
      AbstractVolatile vol = entry.getValue();

      State capabilityState = reconcileStateComponents(capability);

      if (capabilityState != vol.getState()) {
        vol.setState(capabilityState);
      }

      if (capabilityState.isLowerThan(composedState)) {
        composedState = reconcileState(capabilityState, composedState, true);
      }
    }

    return composedState;
  }

  protected State reconcileStateComponents(@Nullable String capability) {
    State newLowestState = State.AVAILABLE;
    State newHighestState = State.UNAVAILABLE;
    boolean atLeastOne = false;

    for (String localKey : components.keySet()) {
      Volatile2 dep = getComponent(localKey);
      if (Objects.isNull(capability) || hasCapability(localKey, dep, capability)) {
        atLeastOne = true;

        if (dep.getState().isLowerThan(newLowestState)) {
          newLowestState = dep.getState();
        }
        if (newHighestState.isLowerThan(dep.getState())) {
          newHighestState = dep.getState();
        }
      }
    }

    if (!atLeastOne) {
      return reconcileStateNoComponents(capability);
    }

    return reconcileState(newLowestState, newHighestState, false);
  }

  protected State reconcileState(State lowest, State highest, boolean allowLimited) {
    if (lowest == State.AVAILABLE) {
      return State.AVAILABLE;
    } else if (allowLimited && State.UNAVAILABLE.isLowerThan(highest)) {
      return State.LIMITED;
    }
    return State.UNAVAILABLE;
  }

  protected State reconcileStateNoComponents(@Nullable String capability) {
    if (LOGGER.isDebugEnabled()) {
      if (Objects.nonNull(capability)) {
        LOGGER.warn(
            "No components with capability '{}' found for volatile: {}",
            capability,
            getUniqueKey());
      } else {
        LOGGER.warn("No components found for volatile: {}", getUniqueKey());
      }
    }
    return State.UNAVAILABLE;
  }

  private boolean hasCapability(String localKey, Volatile2 dep, String capability) {
    return componentCapabilities.get(localKey).contains(capability)
        || (dep instanceof AbstractVolatile
            && ((AbstractVolatile) dep).getVolatileCapabilities().contains(capability));
  }
}
