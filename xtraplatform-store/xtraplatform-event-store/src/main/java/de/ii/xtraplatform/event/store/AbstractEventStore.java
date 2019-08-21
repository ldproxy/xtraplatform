package de.ii.xtraplatform.event.store;

import akka.stream.ActorMaterializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
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
                LOGGER.debug("New event store subscriber: {} {}", subscriber.getEventType(), subscriber);
            }

            EventStream eventStream = getEventStream(subscriber.getEventType());

            eventStream.foreach(subscriber::onEmit);

        }, 10, TimeUnit.SECONDS);
    }

    protected final void emit(MutationEvent event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Emitting event: {} {}", event.type(), event.identifier());
        }
        final EventStream eventStream = getEventStream(event.type());

        eventStream.queue(event);
    }

    protected final void onStart() {
        eventStreams.values()
                    .forEach(eventStream -> emitStateChange(eventStream, StateChangeEvent.STATE.LISTENING));
        this.isStarted = true;
    }

    private synchronized EventStream getEventStream(String eventType) {
        Objects.requireNonNull(eventType, "eventType may not be null");
        return eventStreams.computeIfAbsent(eventType, prefix -> createEventStream());
    }

    private EventStream createEventStream() {
        EventStream eventStream = new EventStream(materializer);

        emitStateChange(eventStream, StateChangeEvent.STATE.REPLAYING);

        // should only happen if there is no replay, so order would be correct
        if (isStarted) {
            emitStateChange(eventStream, StateChangeEvent.STATE.LISTENING);
        }

        return eventStream;
    }

    private void emitStateChange(EventStream eventStream, StateChangeEvent.STATE state) {
        eventStream.queue(ImmutableStateChangeEvent.builder()
                                                   .state(state)
                                                   .build());
    }
}
