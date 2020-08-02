package de.ii.xtraplatform.event.store;

import akka.NotUsed;
import akka.japi.function.Procedure;
import akka.stream.ActorMaterializer;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.SourceQueueWithComplete;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class EventStream {

    private final ActorMaterializer materializer;
    private final CompletableFuture<SourceQueueWithComplete<Event>> eventQueue;
    private final Source<Event, NotUsed> eventStream;
    private CompletableFuture<SourceQueueWithComplete<Event>> eventQueueChain;
    private final String eventType;

    EventStream(ActorMaterializer materializer, String eventType) {
        this.eventQueue = new CompletableFuture<>();
        this.eventStream = Source.<Event>queue(1024, OverflowStrategy.backpressure())
                .mapMaterializedValue(queue -> {
                    eventQueue.complete(queue);
                    return NotUsed.getInstance();
                });
        this.materializer = materializer;
        this.eventQueueChain = eventQueue;
        this.eventType = eventType;
    }

    public void foreach(Consumer<Event> eventConsumer) {
        eventStream.runForeach((Procedure<Event>) eventConsumer::accept, materializer);
    }

    public synchronized void queue(Event event) {
        //eventQueue = eventQueue.thenComposeAsync(queue -> queue.offer(event).handleAsync((queueOfferResult, throwable) -> queue));
        //TODO: to apply backpressure join queue as well as offer; but then we block indefinitely if there is no subscriber for a pathPrefix
        eventQueueChain = eventQueueChain.thenApply(queue -> {
            queue.offer(event);
            return queue;
        });
    }

    public String getEventType() {
        return eventType;
    }
}
