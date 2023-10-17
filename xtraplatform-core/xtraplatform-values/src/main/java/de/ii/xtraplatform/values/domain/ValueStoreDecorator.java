/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.domain;

import com.google.common.collect.ObjectArrays;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface ValueStoreDecorator<T, U extends T> extends KeyValueStore<U> {

  KeyValueStore<T> getDecorated();

  List<String> getValueType();

  default String[] transformPath(String... path) {
    if (path.length >= getValueType().size()
        && Objects.equals(Arrays.asList(path).subList(0, getValueType().size()), getValueType())) {
      return path;
    }

    return ObjectArrays.concat(getValueType().toArray(new String[0]), path, String.class);
  }

  default Identifier transformIdentifier(Identifier identifier) {
    if (identifier.path().size() >= getValueType().size()
        && Objects.equals(identifier.path().subList(0, getValueType().size()), getValueType())) {
      return identifier;
    }
    return ImmutableIdentifier.builder()
        .id(identifier.id())
        .addAllPath(getValueType())
        .addAllPath(identifier.path())
        .build();
  }

  default Identifier transformIdentifierReverse(Identifier identifier) {
    if (identifier.path().isEmpty() || !Objects.equals(identifier.path().get(0), getValueType())) {
      return identifier;
    }
    return ImmutableIdentifier.builder()
        .id(identifier.id())
        .addAllPath(identifier.path().subList(1, identifier.path().size()))
        .build();
  }

  @Override
  default List<Identifier> identifiers(String... path) {
    return getDecorated().identifiers(transformPath(path)).stream()
        .map(this::transformIdentifierReverse)
        .collect(Collectors.toList());
  }

  @Override
  default boolean has(Identifier identifier) {
    return getDecorated().has(transformIdentifier(identifier));
  }

  @Override
  default boolean has(Predicate<Identifier> matcher) {
    throw new IllegalStateException();
  }

  @Override
  default U get(Identifier identifier) {
    return (U) getDecorated().get(transformIdentifier(identifier));
  }

  @Override
  default CompletableFuture<U> put(Identifier identifier, U value) {
    return getDecorated().put(transformIdentifier(identifier), value).thenApply(t -> (U) t);
  }

  @Override
  default CompletableFuture<Boolean> delete(Identifier identifier) {
    return getDecorated().delete(transformIdentifier(identifier));
  }

  @Override
  default long lastModified(Identifier identifier) {
    return getDecorated().lastModified(transformIdentifier(identifier));
  }

  @Override
  default List<String> ids(String... path) {
    return getDecorated().ids(transformPath(path));
  }

  @Override
  default boolean has(String id, String... path) {
    return getDecorated().has(id, transformPath(path));
  }

  @Override
  default U get(String id, String... path) {
    return (U) getDecorated().get(id, transformPath(path));
  }

  @Override
  default CompletableFuture<U> put(String id, U value, String... path) {
    return getDecorated().put(id, value, transformPath(path)).thenApply(t -> (U) t);
  }

  @Override
  default CompletableFuture<Boolean> delete(String id, String... path) {
    return getDecorated().delete(id, transformPath(path));
  }

  @Override
  default long lastModified(String id, String... path) {
    return getDecorated().lastModified(id, transformPath(path));
  }
}
