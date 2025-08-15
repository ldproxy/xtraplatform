/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.domain;

import com.google.common.base.Splitter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public interface KeyValueStore<T> extends Values<T> {

  static String valueType(Identifier identifier) {
    if (identifier.path().isEmpty()) {
      throw new IllegalArgumentException("Invalid path, no value type found.");
    }
    return identifier.path().get(0);
  }

  Splitter TYPE_SPLITTER = Splitter.on('/');

  static boolean valueTypeMatches(Identifier identifier, String type) {
    List<String> valueType = TYPE_SPLITTER.splitToList(type);

    return identifier.path().size() >= valueType.size()
        && Objects.equals(identifier.path().subList(0, valueType.size()), valueType);
  }

  default CompletableFuture<T> put(String id, T value, String... path) {
    return put(Identifier.from(id, path), value);
  }

  default CompletableFuture<Boolean> delete(String id, String... path) {
    return delete(Identifier.from(id, path));
  }

  CompletableFuture<T> put(Identifier identifier, T value);

  CompletableFuture<Boolean> delete(Identifier identifier);

  String hash(T value);
}
