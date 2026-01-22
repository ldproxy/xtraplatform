/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO
public final class RegistryState<T> implements Registry.State<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegistryState.class);
  private static final Joiner JOINER = Joiner.on('.').skipNulls();

  private final String componentName;
  private final ImmutableList<String> componentProperties;
  private final Map<String, T> items;

  public RegistryState(String name, String... componentProperties) {
    this.componentName = name;
    this.componentProperties =
        componentProperties.length > 0
            ? ImmutableList.copyOf(componentProperties)
            : ImmutableList.of("instance.name");
    this.items = new ConcurrentHashMap<>();
  }

  @Override
  public Collection<T> get() {
    return items.values();
  }

  @Override
  public Optional<T> get(String... componentPropertyValues) {
    return Optional.ofNullable(items.get(JOINER.join(componentPropertyValues)));
  }

  @Override
  @SuppressWarnings({"PMD.AvoidSynchronizedAtMethodLevel", "PMD.AvoidDeeplyNestedIfStmts"})
  public synchronized Optional<T> onArrival(T ref) {
    if (Objects.nonNull(ref)) {
      Optional<String> identifier = getComponentIdentifier(ref, componentProperties);
      T service = ref; // bundleContext.getService(ref);

      if (identifier.isPresent()) {
        this.items.put(identifier.get(), service);

        if (LOGGER.isDebugEnabled(MARKER.DI)) {
          LOGGER.debug(MARKER.DI, "Registered {}: {}", componentName, identifier.get());
        }
      }

      return Optional.ofNullable(service);
    }

    return Optional.empty();
  }

  @Override
  @SuppressWarnings({"PMD.AvoidSynchronizedAtMethodLevel", "PMD.AvoidDeeplyNestedIfStmts"})
  public synchronized Optional<T> onDeparture(T ref) {
    if (Objects.nonNull(ref)) {
      Optional<String> identifier = getComponentIdentifier(ref, componentProperties);

      if (identifier.isPresent()) {
        this.items.remove(identifier.get());

        if (LOGGER.isDebugEnabled(MARKER.DI)) {
          LOGGER.debug(MARKER.DI, "Deregistered {}: {}", componentName, identifier.get());
        }
      }
    }

    return Optional.empty();
  }

  private Optional<String> getComponentIdentifier(T component, List<String> properties) {
    final String identifier =
        properties.stream()
            .map(property -> getComponentProperty(component, property))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining("."));

    return Optional.ofNullable(Strings.emptyToNull(identifier));
  }

  private Optional<String> getComponentProperty(T component, String property) {
    if (Objects.isNull(component) || Objects.isNull(Strings.emptyToNull(property))) {
      return Optional.empty();
    }
    /*try {

      return Optional.ofNullable(
              (PropertyDescription[]) component.getProperty("component.properties"))
          .flatMap(
              a ->
                  Arrays.stream(a)
                      .filter(pd -> Objects.nonNull(pd) && Objects.equals(pd.getName(), property))
                      .map(PropertyDescription::getValue)
                      .findFirst())
          .or(() -> Optional.ofNullable((String) component.getProperty(property)));
    } catch (Throwable e) {
      // ignore
    }*/
    return Optional.empty();
  }
}
