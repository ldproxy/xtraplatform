/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain.maptobuilder;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import java.util.AbstractMap;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
// @JsonSerialize(as = Map.class)
@JsonDeserialize(as = ImmutableBuildableMap.class, builder = ImmutableBuildableMap.Builder.class)
public abstract class BuildableMap<T extends Buildable<T>, U extends BuildableBuilder<T>>
    extends ForwardingMap<String, T> {

  public abstract static class Builder {}

  @Value.Derived
  Map<String, T> getDelegate() {
    return /*Stream.concat(
           getInstances().entrySet()
                         .stream(),
           */ getBuilders().entrySet().stream()
        .map(
            entry ->
                new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue().build()))
        // )
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  abstract Map<String, U> getBuilders();

  // abstract Map<String, T> getInstances();

  @Override
  protected Map<String, T> delegate() {
    return getDelegate();
  }
}
