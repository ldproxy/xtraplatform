/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.domain;

import com.google.common.collect.ImmutableMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface KeyValueStore<T> {

  static String valueType(Identifier identifier) {
    if (identifier.path().isEmpty()) {
      throw new IllegalArgumentException("Invalid path, no value type found.");
    }
    return identifier.path().get(0);
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

  default CompletableFuture<T> put(String id, T value, String... path) {
    return put(Identifier.from(id, path), value);
  }

  default CompletableFuture<Boolean> delete(String id, String... path) {
    return delete(Identifier.from(id, path));
  }

  List<Identifier> identifiers(String... path);

  boolean has(Identifier identifier);

  boolean has(Predicate<Identifier> matcher);

  T get(Identifier identifier);

  CompletableFuture<T> put(Identifier identifier, T value);

  CompletableFuture<Boolean> delete(Identifier identifier);
}
