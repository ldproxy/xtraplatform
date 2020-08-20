package de.ii.xtraplatform.store.domain;

import de.ii.xtraplatform.store.app.EventSourcingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
//TODO
public interface EventSourcedKeyValueStore<T> extends EventSourcedStore<T>, KeyValueStore<T>  {

    Logger LOGGER = LoggerFactory.getLogger(EventSourcedKeyValueStore.class);

    EventSourcingCache<T> getEventSourcing();

    @Override
    default void onEmit(Event event) {
        //TODO: when isReplay switches, notify EntityInstantiator
        if (event instanceof MutationEvent) {
            getEventSourcing().onEmit((MutationEvent) event);

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
