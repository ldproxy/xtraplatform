/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.domain;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.base.domain.resiliency.Volatile2;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface Values<T> extends Volatile2 {

  List<Identifier> identifiers(String... path);

  boolean has(Identifier identifier);

  boolean has(Predicate<Identifier> matcher);

  T get(Identifier identifier);

  default long lastModified(Identifier identifier) {
    return -1;
  }

  default List<String> ids(String... path) {
    return identifiers(path).stream().map(Identifier::id).collect(Collectors.toList());
  }

  default Map<String, T> asMap(String... path) {
    return identifiers(path).stream()
        .map(identifier -> new SimpleImmutableEntry<>(identifier.asPath(), get(identifier)))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  default boolean has(String id, String... path) {
    return Objects.nonNull(id) && has(Identifier.from(id, path));
  }

  default T get(String id, String... path) {
    return get(Identifier.from(id, path));
  }

  default long lastModified(String id, String... path) {
    return lastModified(Identifier.from(id, path));
  }
}
