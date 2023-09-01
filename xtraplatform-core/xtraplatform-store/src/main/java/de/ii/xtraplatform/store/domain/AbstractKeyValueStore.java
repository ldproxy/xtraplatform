/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain;

import de.ii.xtraplatform.store.app.EventSourcing;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractKeyValueStore<T> implements KeyValueStore<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractKeyValueStore.class);

  protected abstract EventSourcing<T> getEventSourcing();

  protected CompletableFuture<Void> onListenStart() {
    return CompletableFuture.completedFuture(null);
  }

  protected CompletableFuture<Void> onCreate(Identifier identifier, T entityData) {
    return CompletableFuture.completedFuture(null);
  }

  protected CompletableFuture<Void> onUpdate(Identifier identifier, T entityData) {
    return CompletableFuture.completedFuture(null);
  }

  protected void onDelete(Identifier identifier) {}

  protected void onFailure(Identifier identifier, Throwable throwable) {}

  @Override
  public List<String> ids(String... path) {
    return identifiers(path).stream().map(Identifier::id).collect(Collectors.toList());
  }

  @Override
  public boolean has(String id, String... path) {
    return Objects.nonNull(id) && has(Identifier.from(id, path));
  }

  @Override
  public T get(String id, String... path) {
    return get(Identifier.from(id, path));
  }

  @Override
  public CompletableFuture<T> put(String id, T value, String... path) {
    return put(Identifier.from(id, path), value);
  }

  @Override
  public CompletableFuture<Boolean> delete(String id, String... path) {
    return drop(Identifier.from(id, path));
  }

  @Override
  public List<Identifier> identifiers(String... path) {
    return getEventSourcing().getIdentifiers(path);
  }

  @Override
  public boolean has(Identifier identifier) {
    return getEventSourcing().isInCache(identifier);
  }

  @Override
  public boolean has(Predicate<Identifier> matcher) {
    return getEventSourcing().isInCache(matcher);
  }

  @Override
  public T get(Identifier identifier) {
    return getEventSourcing().getFromCache(identifier);
  }

  @Override
  public CompletableFuture<T> put(Identifier identifier, T value) {
    boolean exists = has(identifier);

    return getEventSourcing()
        .pushMutationEvent(identifier, value)
        .whenComplete(
            (entityData, throwable) -> {
              if (Objects.nonNull(throwable)) {
                onFailure(identifier, throwable);
              } else if (Objects.nonNull(entityData)) {
                if (exists) onUpdate(identifier, entityData);
                else onCreate(identifier, entityData).join();
              }
            });
  }

  protected CompletableFuture<T> putWithoutTrigger(Identifier identifier, T value) {
    return getEventSourcing().pushMutationEvent(identifier, value);
  }

  protected CompletableFuture<T> putPartialWithoutTrigger(
      Identifier identifier, Map<String, Object> value) {
    return getEventSourcing().pushPartialMutationEvent(identifier, value);
  }

  protected CompletableFuture<Boolean> drop(Identifier identifier) {
    return getEventSourcing()
        .pushMutationEvent(identifier, null)
        .whenComplete(
            (entityData, throwable) -> {
              if (Objects.nonNull(throwable)) {
                onFailure(identifier, throwable);
              } else if (Objects.isNull(entityData)) {
                onDelete(identifier);
              }
            })
        .thenApply(Objects::isNull);
  }

  protected CompletableFuture<Boolean> dropWithoutTrigger(Identifier identifier) {
    return getEventSourcing().pushMutationEvent(identifier, null).thenApply(Objects::isNull);
  }
}
