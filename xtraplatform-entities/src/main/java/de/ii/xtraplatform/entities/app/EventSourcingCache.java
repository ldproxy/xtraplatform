/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.app;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.entities.domain.EntityEvent;
import de.ii.xtraplatform.entities.domain.EventStore;
import de.ii.xtraplatform.entities.domain.ImmutableMutationEvent;
import de.ii.xtraplatform.values.domain.Identifier;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventSourcingCache<T> {

  private final Map<Identifier, T> cache;
  private final Map<Identifier, CompletableFuture<T>> queue;
  private final EventStore eventStore;
  private final String eventType;
  private final Function<T, byte[]> serializer;
  private final Deserializer<T> deserializer;
  private final String defaultFormat;
  private static final Logger LOGGER = LoggerFactory.getLogger(EventSourcingCache.class);

  public interface Deserializer<T> {
    T deserialize(Identifier identifier, byte[] payload, String format);
  }

  public EventSourcingCache(
      EventStore eventStore,
      String eventType,
      Function<T, byte[]> serializer,
      Deserializer<T> deserializer,
      Supplier<String> defaultFormat) {
    this.eventStore = eventStore;
    this.eventType = eventType;
    this.cache = new ConcurrentHashMap<>();
    this.queue = new ConcurrentHashMap<>();
    this.serializer = serializer;
    this.deserializer = deserializer;
    this.defaultFormat = defaultFormat.get();
  }

  public boolean isInCache(Identifier identifier) {
    return cache.containsKey(identifier);
  }

  public T getFromCache(Identifier identifier) {
    return cache.get(identifier);
  }

  public List<Identifier> getIdentifiers(String... path) {
    return cache.keySet().stream()
        .filter(
            identifier ->
                path.length == 0 || Objects.equals(ImmutableList.copyOf(path), identifier.path()))
        .collect(Collectors.toList());
  }

  public CompletableFuture<T> pushMutationEvent(Identifier identifier, T data) {
    final byte[] payload = serializer.apply(data);

    return pushMutationEventRaw(identifier, payload, Objects.isNull(data));
  }

  public CompletableFuture<T> pushMutationEventRaw(Identifier identifier, byte[] payload) {
    return pushMutationEventRaw(identifier, payload, false);
  }

  private CompletableFuture<T> pushMutationEventRaw(
      Identifier identifier, byte[] payload, boolean isDelete) {
    final CompletableFuture<T> completableFuture = new CompletableFuture<>();

    try {
      // TODO: if already in queue, pipeline to existing future
      final EntityEvent entityEvent =
          ImmutableMutationEvent.builder()
              .type(eventType)
              .identifier(identifier)
              .payload(payload)
              .deleted(isDelete ? true : null)
              .format(defaultFormat)
              .build();

      queue.put(identifier, completableFuture);

      // TODO: pass snapshot to push, event store can decide what to do with it
      // who decides if snapshotting is enabled?
      eventStore.push(entityEvent);

    } catch (Throwable e) {
      completableFuture.completeExceptionally(e);
      return completableFuture;
    }

    return completableFuture;
  }

  public void onEmit(EntityEvent event) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Adding event: {} {}", event.type(), event.identifier());
    }

    T value = deserializer.deserialize(event.identifier(), event.payload(), event.format());

    if (Objects.isNull(value)) {
      cache.remove(event.identifier());
    } else {
      cache.put(event.identifier(), value);
    }

    if (queue.containsKey(event.identifier())) {
      queue.remove(event.identifier()).complete(value);
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Added value: {}", value);
      // LOGGER.trace("CACHE {}", cache);
    }
  }
}
