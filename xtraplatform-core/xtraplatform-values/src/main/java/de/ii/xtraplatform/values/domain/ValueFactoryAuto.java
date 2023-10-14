/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.domain;

import de.ii.xtraplatform.values.domain.ValueEncoding.FORMAT;
import java.util.Objects;

public class ValueFactoryAuto implements ValueFactory {

  private final Class<? extends Value> valueClass;
  private final de.ii.xtraplatform.values.domain.annotations.Value value;

  protected <T extends Value> ValueFactoryAuto(Class<T> clazz) {
    this.valueClass = clazz;
    this.value =
        Objects.requireNonNull(
            clazz.getAnnotation(de.ii.xtraplatform.values.domain.annotations.Value.class),
            "Missing annotation @Value for class " + clazz);
  }

  @Override
  public Class<? extends Value> valueClass() {
    return valueClass;
  }

  @Override
  public String type() {
    return value.type();
  }

  @Override
  public Builder<? extends Value> builder() {
    try {
      return (Builder<? extends Value>) value.builder().newInstance();
    } catch (Throwable e) {
      throw new IllegalStateException(
          "Invalid builder " + value.builder() + " in @Value annotation with type=" + type(), e);
    }
  }

  @Override
  public boolean cacheValues() {
    return value.cacheValues();
  }

  @Override
  public FORMAT defaultFormat() {
    return value.defaultFormat();
  }
}
