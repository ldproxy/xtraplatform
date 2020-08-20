package de.ii.xtraplatform.store.domain;

import de.ii.xtraplatform.store.app.EventSourcingCache;

public interface EventSourcing {

    <T> EventSourcingCache<T> createCache(EventSourcedStore<T> store);
}
