/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.values.domain.ValueEncoding.FORMAT;
import de.ii.xtraplatform.values.domain.annotations.FromValueStore;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ValueFactoryAuto implements ValueFactory {

  private final Class<? extends StoredValue> valueClass;
  private final FromValueStore options;
  private final JsonDeserialize deserialize;

  protected <T extends StoredValue> ValueFactoryAuto(Class<T> clazz) {
    this.valueClass = clazz;
    this.options =
        Objects.requireNonNull(
            clazz.getAnnotation(FromValueStore.class),
            "Missing annotation @Value for class " + clazz);
    this.deserialize =
        Objects.requireNonNull(
            clazz.getAnnotation(JsonDeserialize.class),
            "Missing annotation @JsonDeserialize for class " + clazz);
  }

  @Override
  public Class<? extends StoredValue> valueClass() {
    return valueClass;
  }

  @Override
  public String type() {
    return options.type();
  }

  @Override
  public ValueBuilder<? extends StoredValue> builder() {
    try {
      return (ValueBuilder<? extends StoredValue>) deserialize.builder().newInstance();
    } catch (Throwable e) {
      throw new IllegalStateException(
          "Invalid builder "
              + deserialize.builder()
              + " in @JsonDeserialize annotation for class "
              + valueClass,
          e);
    }
  }

  @Override
  public boolean cacheValues() {
    return options.cacheValues();
  }

  @Override
  public FORMAT defaultFormat() {
    return options.defaultFormat();
  }

  @Override
  public Map<String, FORMAT> formatAliases() {
    return Arrays.stream(options.formatAliases())
        .map(alias -> Map.entry(alias.extension(), alias.format()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
