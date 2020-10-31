/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.store.domain.Event;
import de.ii.xtraplatform.store.domain.EventFilter;
import de.ii.xtraplatform.store.domain.EventStore;
import de.ii.xtraplatform.store.domain.EventStoreSubscriber;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.ImmutableMutationEvent;
import de.ii.xtraplatform.store.domain.MutationEvent;
import de.ii.xtraplatform.store.domain.ReloadEvent;
import de.ii.xtraplatform.store.domain.StateChangeEvent;
import de.ii.xtraplatform.store.domain.ValueCache;
import de.ii.xtraplatform.store.domain.ValueEncoding;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: should this really be a facade for EventStore? or can we make it plain ValueCache?
public class EventSourcing<T> implements EventStoreSubscriber, ValueCache<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventSourcing.class);

  private final Map<Identifier, T> cache;
  private final Map<Identifier, CompletableFuture<T>> queue;
  private final EventStore eventStore;
  private final List<String> eventTypes;
  private final ValueEncoding<T> valueEncoding;
  private final Supplier<CompletableFuture<Void>> onStart;
  private final Optional<Function<MutationEvent, List<MutationEvent>>> eventProcessor;
  private final Optional<BiConsumer<Identifier, T>> updateHook;
  private final Optional<BiConsumer<Identifier, T>> valueValidator;
  private final Set<String> started;

  public EventSourcing(
          EventStore eventStore,
          List<String> eventTypes,
          ValueEncoding<T> valueEncoding,
          Supplier<CompletableFuture<Void>> onStart,
          Optional<Function<MutationEvent, List<MutationEvent>>> eventProcessor, Optional<BiConsumer<Identifier, T>> updateHook) {
    this(eventStore, eventTypes, valueEncoding, onStart, eventProcessor, updateHook, Optional.empty());
  }

  public EventSourcing(
          EventStore eventStore,
          List<String> eventTypes,
          ValueEncoding<T> valueEncoding,
          Supplier<CompletableFuture<Void>> onStart,
          Optional<Function<MutationEvent, List<MutationEvent>>> eventProcessor, Optional<BiConsumer<Identifier, T>> updateHook, Optional<BiConsumer<Identifier, T>> valueValidator) {
    this.eventStore = eventStore;
    this.eventTypes = eventTypes;
    this.eventProcessor = eventProcessor;
    this.updateHook = updateHook;
    this.cache = new ConcurrentSkipListMap<>();
    this.queue = new ConcurrentHashMap<>();
    this.valueEncoding = valueEncoding;
    this.onStart = onStart;
    this.valueValidator = valueValidator;
    this.started = new HashSet<>();

    eventStore.subscribe(this);
  }

  @Override
  public List<String> getEventTypes() {
    return eventTypes;
  }

  @Override
  public void onEmit(Event event) {
    if (event instanceof MutationEvent) {
      try {
        if (eventProcessor.isPresent()) {
          for (MutationEvent mutationEvent : eventProcessor.get()
                                                           .apply((MutationEvent) event)) {
            onEmit(mutationEvent);
          }
        } else {
          onEmit((MutationEvent) event);
        }
      } catch (Throwable e) {
        LOGGER.error("Cannot load '{}': {}", ((MutationEvent) event).asPath(), e.getMessage());
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Stacktrace:", e);
        }
      }

    } else if (event instanceof StateChangeEvent) {
      switch (((StateChangeEvent) event).state()) {
        case REPLAYING:
          LOGGER.debug("Replaying events for {}", ((StateChangeEvent) event).type());
          break;
        case LISTENING:
          started.add(((StateChangeEvent) event).type());

          if (started.containsAll(getEventTypes())) {
            onStart
                .get()
                .thenRun(
                    () ->
                        LOGGER.debug(
                            "Listening for events for {}", started));
          }
          break;
      }
    } else if (event instanceof ReloadEvent && updateHook.isPresent()) {
      List<Identifier> identifiers = getIdentifiers(((ReloadEvent) event).filter());
      identifiers.forEach(identifier -> {
        T data = getFromCache(identifier);
        updateHook.get()
                  .accept(identifier, data);
      });
    }
  }

  @Override
  public boolean isInCache(Identifier identifier) {
    return cache.containsKey(identifier);
  }

  @Override
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
    final byte[] payload = valueEncoding.serialize(data);

    return pushMutationEventRaw(identifier, payload, Objects.isNull(data));
  }

  public CompletableFuture<T> pushPartialMutationEvent(Identifier identifier, Map<String, Object> data) {
    final byte[] payload = valueEncoding.serialize(data);

    return pushMutationEventRaw(identifier, payload, Objects.isNull(data));
  }

  public CompletableFuture<T> pushMutationEventRaw(Identifier identifier, byte[] payload) {
    return pushMutationEventRaw(identifier, payload, false);
  }

  // TODO: which eventType should we push?
  private CompletableFuture<T> pushMutationEventRaw(
      Identifier identifier, byte[] payload, boolean isDelete) {
    final CompletableFuture<T> completableFuture = new CompletableFuture<>();

    try {
      // TODO: if already in queue, pipeline to existing future
      final MutationEvent mutationEvent =
          ImmutableMutationEvent.builder()
              .type(eventTypes.get(0))
              .identifier(identifier)
              .payload(payload)
              .deleted(isDelete ? true : null)
              .format(valueEncoding.getDefaultFormat().toString())
              .build();

      queue.put(identifier, completableFuture);

      // TODO: pass snapshot to push, event store can decide what to do with it
      // who decides if snapshotting is enabled?
      eventStore.push(mutationEvent);

    } catch (Throwable e) {
      completableFuture.completeExceptionally(e);
      return completableFuture;
    }

    return completableFuture;
  }

  private void onEmit(MutationEvent event) throws Throwable {
    Identifier key = event.identifier();
    ValueEncoding.FORMAT payloadFormat = ValueEncoding.FORMAT.fromString(event.format());

    if (payloadFormat == ValueEncoding.FORMAT.UNKNOWN) {
      if (queue.containsKey(key)) {
        queue.remove(key).complete(null);
      }
      return;
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Adding event: {} {} {}", event.type(), event.identifier(), event.format());
    }

    T value;
    Throwable error = null;

    try {
      value = valueEncoding.deserialize(event.identifier(), event.payload(), payloadFormat);

    } catch (Throwable e) {
      error = e;
      value = cache.getOrDefault(key, null);
    }

    if (Objects.isNull(error) && !Objects.isNull(value) && valueValidator.isPresent()) {
      try {
        valueValidator.get().accept(key, value);
      } catch (Throwable e) {
        error = e.getCause();
        value = cache.getOrDefault(key, null);
      }
    }

    if (Objects.isNull(value)) {
      cache.remove(key);
    } else {
      cache.put(key, value);
    }

    if (queue.containsKey(key)) {
      queue.remove(key).complete(value);
    }

    if (!Objects.isNull(error)) {
      throw error;
    }
  }

  private List<Identifier> getIdentifiers(EventFilter filter) {

    return getIdentifiers().stream().filter(identifier -> {
      if (filter.getEntityTypes().contains("*") || (!identifier.path().isEmpty() && filter.getEntityTypes().contains(identifier.path().get(0)))) {
        return filter.getIds().contains("*") || filter.getIds().contains(identifier.id());
      }

      return false;
    }).collect(Collectors.toList());
  }
}
