/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableSet;
import dagger.Lazy;
import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.StoredValue;
import de.ii.xtraplatform.values.domain.ValueFactories;
import de.ii.xtraplatform.values.domain.ValueFactory;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class ValueFactoriesImpl implements ValueFactories {

  private final Lazy<Set<ValueFactory>> valueFactories;

  @Inject
  public ValueFactoriesImpl(Lazy<Set<ValueFactory>> valueFactories) {
    this.valueFactories = valueFactories;
  }

  @Override
  public ValueFactory get(String valueType) {
    return valueFactories.get().stream()
        .filter(valueFactory -> valueFactory.type().equalsIgnoreCase(valueType))
        .findFirst()
        .orElseThrow(
            () ->
                new NoSuchElementException(
                    String.format("No factory found for value type %s", valueType)));
  }

  @Override
  public ValueFactory get(Identifier identifier) {
    return valueFactories.get().stream()
        .filter(valueFactory -> identifier.asPath().startsWith(valueFactory.type()))
        .findFirst()
        .orElseThrow(
            () ->
                new NoSuchElementException(
                    String.format("No factory found for value identifier %s", identifier)));
  }

  @Override
  public ValueFactory get(Class<? extends StoredValue> valueClass) {
    return valueFactories.get().stream()
        .filter(entityFactory -> Objects.equals(valueClass, entityFactory.valueClass()))
        .findFirst()
        .orElseThrow(
            () -> new NoSuchElementException("No factory found for value class " + valueClass));
  }

  @Override
  public Set<String> getTypes() {
    return valueFactories.get().stream()
        .map(ValueFactory::type)
        .collect(ImmutableSet.toImmutableSet());
  }
}
