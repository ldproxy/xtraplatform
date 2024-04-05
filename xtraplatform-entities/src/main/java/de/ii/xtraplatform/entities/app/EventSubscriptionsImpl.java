/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.app;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.entities.domain.EntityEvent;
import de.ii.xtraplatform.entities.domain.EventStoreSubscriber;
import de.ii.xtraplatform.entities.domain.ImmutableStateChangeEvent;
import de.ii.xtraplatform.entities.domain.StateChangeEvent;
import de.ii.xtraplatform.entities.domain.TypedEvent;
import de.ii.xtraplatform.streams.domain.Event;
import de.ii.xtraplatform.streams.domain.EventStream;
import de.ii.xtraplatform.streams.domain.Reactive;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventSubscriptionsImpl implements EventSubscriptions {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventSubscriptionsImpl.class);

  private final Map<String, EventStream<Event>> eventStreams;
  private final Reactive.Runner streamRunner;
  private final ExecutorService executorService;
  private boolean isStarted;

  protected EventSubscriptionsImpl(Reactive.Runner streamRunner) {
    this.eventStreams = new ConcurrentHashMap<>();
    this.streamRunner = streamRunner;
    this.executorService =
        MoreExecutors.getExitingExecutorService(
            (ThreadPoolExecutor)
                Executors.newFixedThreadPool(
                    1, new ThreadFactoryBuilder().setNameFormat("events-%d").build()));
  }

  @Override
  public void addSubscriber(EventStoreSubscriber subscriber) {
    executorService.submit(
        () -> {
          Thread.currentThread().setName("startup");

          if (LOGGER.isDebugEnabled(MARKER.DI)) {
            LOGGER.debug(
                MARKER.DI,
                "New event store subscriber: {} {}",
                subscriber.getEventTypes(),
                subscriber);
          }

          for (String eventType : subscriber.getEventTypes()) {
            EventStream<Event> eventStream = getEventStream(eventType);
            CompletableFuture<Void> cmp = new CompletableFuture<>();
            eventStream.foreach(
                event -> {
                  if (event instanceof StateChangeEvent
                      && ((StateChangeEvent) event).state() == StateChangeEvent.STATE.LISTENING) {
                    subscriber.onEmit(event);
                    if (LOGGER.isTraceEnabled()) {
                      LOGGER.trace("{} STARTED LISTENER", eventType);
                    }
                    cmp.complete(null);
                    return;
                  }

                  // only emit one event at a time
                  synchronized (streamRunner) {
                    if (LOGGER.isTraceEnabled() && event instanceof EntityEvent) {
                      LOGGER.trace(
                          "EMIT: {} {}",
                          ((EntityEvent) event).type(),
                          ((EntityEvent) event).identifier());
                    }
                    subscriber.onEmit(event);
                    if (LOGGER.isTraceEnabled() && event instanceof EntityEvent) {
                      LOGGER.trace(
                          "EMITTED: {} {}",
                          ((EntityEvent) event).type(),
                          ((EntityEvent) event).identifier());
                    }
                  }
                });
            cmp.join();
            // LOGGER.debug("NEXT");
          }
        });
  }

  @Override
  public synchronized void emitEvent(TypedEvent event) {
    if (LOGGER.isTraceEnabled() && event instanceof EntityEvent) {
      LOGGER.trace("Emitting event: {} {}", event.type(), ((EntityEvent) event).identifier());
    }
    final EventStream<Event> eventStream = getEventStream(event.type());

    eventStream.queue(event);
  }

  @Override
  public void startListening() {
    eventStreams
        .values()
        .forEach(
            eventStream ->
                emitStateChange(
                    eventStream, StateChangeEvent.STATE.LISTENING, eventStream.getEventType()));
    this.isStarted = true;
  }

  private synchronized EventStream<Event> getEventStream(String eventType) {
    Objects.requireNonNull(eventType, "eventType may not be null");
    return eventStreams.computeIfAbsent(eventType, prefix -> createEventStream(eventType));
  }

  private EventStream<Event> createEventStream(String eventType) {
    EventStream<Event> eventStream = new EventStream<>(streamRunner, eventType);

    emitStateChange(eventStream, StateChangeEvent.STATE.REPLAYING, eventType);

    // should only happen if there is no replay, so order would be correct
    if (isStarted) {
      emitStateChange(eventStream, StateChangeEvent.STATE.LISTENING, eventType);
    }

    return eventStream;
  }

  private void emitStateChange(
      EventStream<Event> eventStream, StateChangeEvent.STATE state, String type) {
    eventStream.queue(ImmutableStateChangeEvent.builder().state(state).type(type).build());
  }
}
