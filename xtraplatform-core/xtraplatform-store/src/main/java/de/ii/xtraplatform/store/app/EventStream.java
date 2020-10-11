/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app;

import akka.NotUsed;
import akka.japi.function.Procedure;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.SourceQueueWithComplete;
import de.ii.xtraplatform.store.domain.Event;
import de.ii.xtraplatform.streams.app.StreamExecutorServiceConfigurator;
import de.ii.xtraplatform.streams.domain.StreamRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class EventStream {

  private final StreamRunner streamRunner;
  private final CompletableFuture<SourceQueueWithComplete<Event>> eventQueue;
  private final Source<Event, NotUsed> eventStream;
  private CompletableFuture<SourceQueueWithComplete<Event>> eventQueueChain;
  private final String eventType;

  public EventStream(StreamRunner streamRunner, String eventType) {
    this.eventQueue = new CompletableFuture<>();
    this.eventStream =
        Source.<Event>queue(1024, OverflowStrategy.backpressure())
            .mapMaterializedValue(
                queue -> {
                  eventQueue.complete(queue);
                  return NotUsed.getInstance();
                });
    this.streamRunner = streamRunner;
    this.eventQueueChain = eventQueue;
    this.eventType = eventType;
  }

  public void foreach(Consumer<Event> eventConsumer) {
    streamRunner.runForeach(eventStream, (Procedure<Event>) eventConsumer::accept);
  }

  public synchronized void queue(Event event) {
    // eventQueue = eventQueue.thenComposeAsync(queue ->
    // queue.offer(event).handleAsync((queueOfferResult, throwable) -> queue));
    // TODO: to apply backpressure join queue as well as offer; but then we block indefinitely if
    // there is no subscriber for a pathPrefix
    eventQueueChain =
        eventQueueChain.thenApply(
            queue -> {
              queue.offer(event);
              return queue;
            });
  }

  public String getEventType() {
    return eventType;
  }
}
