package de.ii.xtraplatform.event.store;

public interface EventStore {

    void subscribe(EventStoreSubscriber subscriber);

    void push(MutationEvent event);

    boolean isReadOnly();
}
