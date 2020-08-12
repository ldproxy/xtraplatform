package de.ii.xtraplatform.store.domain;

import java.util.concurrent.CompletableFuture;

public interface EventSourcedStore<T> extends EventStoreSubscriber {

    default String getDefaultFormat() {
        return null;
    }

    default CompletableFuture<Void> onStart() {
        return CompletableFuture.completedFuture(null);
    }

    byte[] serialize(T value);

    T deserialize(Identifier identifier, byte[] payload, String format);
}
