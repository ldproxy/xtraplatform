/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import de.ii.xtraplatform.entities.app.EventSourcing;
import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.ValueStore;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractKeyValueStore<T> implements ValueStore<T> {

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

  @Override
  public CompletableFuture<Boolean> delete(Identifier identifier) {
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
