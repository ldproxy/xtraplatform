/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import com.codahale.metrics.health.HealthCheck;
import de.ii.xtraplatform.base.domain.resiliency.Volatile2;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry.ChangeHandler;
import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.ValueEncoding;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("PMD.TooManyMethods")
public interface EntityStoreDecorator<T extends EntityData, U extends T>
    extends EntityDataStore<U>, Volatile2 {

  EntityDataStore<T> getDecorated();

  String[] transformPath(String... path);

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
    return (CompletableFuture<U>) getDecorated().put(id, value, transformPath(path));
  }

  @Override
  default CompletableFuture<Boolean> delete(String id, String... path) {
    return getDecorated().delete(id, transformPath(path));
  }

  @Override
  default String hash(U value) {
    return getDecorated().hash(value);
  }

  @Override
  default CompletableFuture<U> patch(String id, Map<String, Object> partialData, String... path) {
    return (CompletableFuture<U>) getDecorated().patch(id, partialData, transformPath(path));
  }

  @Override
  default CompletableFuture<U> patch(
      String id, Map<String, Object> partialData, boolean skipLastModified, String... path) {
    return (CompletableFuture<U>)
        getDecorated().patch(id, partialData, skipLastModified, transformPath(path));
  }

  @Override
  default List<Identifier> identifiers(String... path) {
    return getDecorated().identifiers().stream()
        .filter(
            identifier ->
                Identifier.JOINER
                    .join(identifier.path())
                    .endsWith(Identifier.JOINER.join(transformPath(path))))
        .collect(Collectors.toList());
  }

  // TODO: transformPath
  @Override
  default boolean has(Identifier identifier) {
    return getDecorated().has(identifier);
  }

  @Override
  default boolean has(Predicate<Identifier> matcher) {
    return getDecorated().has(matcher);
  }

  // TODO
  @Override
  default U get(Identifier identifier) {
    return (U) getDecorated().get(identifier);
  }

  // TODO
  @Override
  default CompletableFuture<U> put(Identifier identifier, U value) {
    return null; // getDecorated().patch(identifier, value);
  }

  // TODO
  @Override
  default CompletableFuture<Boolean> delete(Identifier identifier) {
    return null; // getDecorated().delete(identifier);
  }

  @Override
  default <U1 extends U> EntityDataStore<U1> forType(Class<U1> type) {
    throw new IllegalArgumentException();
  }

  @Override
  default EntityDataStore<EntityData> forType(String type) {
    throw new IllegalArgumentException();
  }

  @Override
  default ValueEncoding<EntityData> getValueEncoding() {
    return getDecorated().getValueEncoding();
  }

  @Override
  default Map<String, Object> asMap(Identifier identifier, EntityData entityData)
      throws IOException {
    return getDecorated().asMap(identifier, entityData);
  }

  @Override
  default EntityData fromMap(Identifier identifier, Map<String, Object> entityData)
      throws IOException {
    return getDecorated().fromMap(identifier, entityData);
  }

  @Override
  default EntityData fromBytes(Identifier identifier, byte[] entityData) throws IOException {
    return getDecorated().fromBytes(identifier, entityData);
  }

  @Override
  default EntityDataBuilder<EntityData> getBuilder(
      Identifier identifier, Optional<String> entitySubtype) {
    return getDecorated().getBuilder(identifier, entitySubtype);
  }

  @Override
  default String getUniqueKey() {
    return getDecorated().getUniqueKey();
  }

  @Override
  default State getState() {
    return getDecorated().getState();
  }

  @Override
  default Optional<String> getMessage() {
    return getDecorated().getMessage();
  }

  @Override
  default Runnable onStateChange(ChangeHandler handler, boolean initialCall) {
    return getDecorated().onStateChange(handler, initialCall);
  }

  @Override
  default Optional<HealthCheck> asHealthCheck() {
    return getDecorated().asHealthCheck();
  }
}
