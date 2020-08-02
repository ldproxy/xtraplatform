package de.ii.xtraplatform.event.store;

import akka.stream.ActorMaterializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class AbstractEventStore implements EventStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEventStore.class);

    private final Map<String, EventStream> eventStreams;
    private final ActorMaterializer materializer;
    private final ScheduledExecutorService executorService;
    private boolean isStarted;

    protected AbstractEventStore(ActorMaterializer materializer) {
        this.eventStreams = new ConcurrentHashMap<>();
        this.materializer = materializer;
        this.executorService = new ScheduledThreadPoolExecutor(1);
    }

    //TODO: unsubscribe
    @Override
    public final void subscribe(EventStoreSubscriber subscriber) {
        executorService.schedule(() -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("New event store subscriber: {} {}", subscriber.getEventTypes(), subscriber);
            }

            for (String eventType: subscriber.getEventTypes()) {
                EventStream eventStream = getEventStream(eventType);
                CompletableFuture<Void> cmp = new CompletableFuture<>();
                eventStream.foreach(event -> {
                    if (event instanceof StateChangeEvent && ((StateChangeEvent) event).state() == StateChangeEvent.STATE.LISTENING) {
                        //LOGGER.debug("{} STARTED", eventType);
                        cmp.complete(null);
                    }
                    subscriber.onEmit(event);
                });
                cmp.join();
                //LOGGER.debug("NEXT");
            }

        }, 10, TimeUnit.SECONDS);
    }

    protected final void emit(MutationEvent event) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Emitting event: {} {}", event.type(), event.identifier());
        }
        final EventStream eventStream = getEventStream(event.type());

        eventStream.queue(event);
    }

    protected final void onStart() {
        eventStreams.values()
                    .forEach(eventStream -> emitStateChange(eventStream, StateChangeEvent.STATE.LISTENING, eventStream.getEventType()));
        this.isStarted = true;
    }

    private synchronized EventStream getEventStream(String eventType) {
        Objects.requireNonNull(eventType, "eventType may not be null");
        return eventStreams.computeIfAbsent(eventType, prefix -> createEventStream(eventType));
    }

    private EventStream createEventStream(String eventType) {
        EventStream eventStream = new EventStream(materializer, eventType);

        emitStateChange(eventStream, StateChangeEvent.STATE.REPLAYING, eventType);

        // should only happen if there is no replay, so order would be correct
        if (isStarted) {
            emitStateChange(eventStream, StateChangeEvent.STATE.LISTENING, eventType);
        }

        return eventStream;
    }

    private void emitStateChange(EventStream eventStream, StateChangeEvent.STATE state, String type) {
        eventStream.queue(ImmutableStateChangeEvent.builder()
                                                   .state(state)
                                                   .type(type)
                                                   .build());
    }
}
