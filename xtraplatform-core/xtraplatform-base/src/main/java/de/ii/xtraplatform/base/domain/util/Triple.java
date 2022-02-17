/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain.util;

import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public interface Triple<T, U, V> {

  static <T, U, V> Triple<T, U, V> of(T t, U u, V v) {
    return ImmutableTriple.of(t, u, v);
  }

  @Nullable
  @Value.Parameter
  T first();

  @Nullable
  @Value.Parameter
  U second();

  @Nullable
  @Value.Parameter
  V third();
}
