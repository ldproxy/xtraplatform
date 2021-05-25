/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.streams.domain;

import akka.NotUsed;
import akka.stream.OverflowStrategy;
import akka.stream.QueueOfferResult;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.SourceQueueWithComplete;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class EventStream<T extends Event> {

  private final Reactive.Runner streamRunner;
  private final CompletableFuture<SourceQueueWithComplete<T>> eventQueue;
  private final Source<T, NotUsed> eventStream;
  private CompletableFuture<SourceQueueWithComplete<T>> eventQueueChain;
  private final String eventType;

  public EventStream(Reactive.Runner streamRunner, String eventType) {
    this.eventQueue = new CompletableFuture<>();
    this.eventStream =
        Source.<T>queue(1024, OverflowStrategy.backpressure())
            .mapMaterializedValue(
                queue -> {
                  eventQueue.complete(queue);
                  return NotUsed.getInstance();
                });
    this.streamRunner = streamRunner;
    this.eventQueueChain = eventQueue;
    this.eventType = eventType;
  }

  public void foreach(Consumer<T> eventConsumer) {
    Reactive.RunnableStream<Void> reactiveStream = Reactive.Source.akka(eventStream)
        .to(Reactive.Sink.foreach(eventConsumer))
        .on(streamRunner);
    reactiveStream.run();
    //streamRunner.runForeach(eventStream, (Procedure<T>) eventConsumer::accept);
  }

  public synchronized CompletableFuture<QueueOfferResult> queue(T event) {
    // eventQueue = eventQueue.thenComposeAsync(queue ->
    // queue.offer(event).handleAsync((queueOfferResult, throwable) -> queue));
    // TODO: to apply backpressure join queue as well as offer; but then we block indefinitely if
    // there is no subscriber for a pathPrefix
    CompletableFuture<QueueOfferResult> cmp = new CompletableFuture<>();
    eventQueueChain =
        eventQueueChain.thenApply(
            queue -> {
              queue.offer(event).thenAccept(cmp::complete);
              return queue;
            });

    return cmp;
  }

  public String getEventType() {
    return eventType;
  }
}
