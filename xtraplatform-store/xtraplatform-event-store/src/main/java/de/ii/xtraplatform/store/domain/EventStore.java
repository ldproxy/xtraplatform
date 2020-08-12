package de.ii.xtraplatform.store.domain;

public interface EventStore {

    void subscribe(EventStoreSubscriber subscriber);

    void push(MutationEvent event);

    boolean isReadOnly();
}
