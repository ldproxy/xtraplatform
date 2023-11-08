/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import de.ii.xtraplatform.entities.app.EventSourcingCache;
import de.ii.xtraplatform.streams.domain.Event;
import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.KeyValueStore;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface EventSourcedKeyValueStore<T> extends EventSourcedStore<T>, KeyValueStore<T> {

  Logger LOGGER = LoggerFactory.getLogger(EventSourcedKeyValueStore.class);

  EventSourcingCache<T> getEventSourcing();

  @Override
  default void onEmit(Event event) {
    // TODO: when isReplay switches, notify EntityInstantiator
    if (event instanceof EntityEvent) {
      getEventSourcing().onEmit((EntityEvent) event);

    } else if (event instanceof StateChangeEvent) {
      switch (((StateChangeEvent) event).state()) {
        case REPLAYING:
          LOGGER.debug("Replaying events for {}", getEventTypes());
          break;
        case LISTENING:
          onStart().thenRun(() -> LOGGER.debug("Listening for events for {}", getEventTypes()));
          break;
      }
    }
  }

  @Override
  default List<Identifier> identifiers(String... path) {
    return getEventSourcing().getIdentifiers(path);
  }

  @Override
  default boolean has(Identifier identifier) {
    return getEventSourcing().isInCache(identifier);
  }

  @Override
  default T get(Identifier identifier) {
    return getEventSourcing().getFromCache(identifier);
  }

  @Override
  default CompletableFuture<T> put(Identifier identifier, T value) {
    return getEventSourcing().pushMutationEvent(identifier, value);
  }

  /*@Override
  default CompletableFuture<T> delete(Identifier identifier) {
      return getEventSourcing().pushMutationEvent(identifier, null);
  }*/
}
