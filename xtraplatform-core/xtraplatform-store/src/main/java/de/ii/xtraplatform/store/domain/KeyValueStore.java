package de.ii.xtraplatform.store.domain;

import java.util.List;
import java.util.concurrent.CompletableFuture;

// no de/serialization, no merging, just crd with ids/paths, basically what we have in kvstore-api
// does it need transactions???
public interface KeyValueStore<T> {

    List<String> ids(String... path);

    boolean has(String id, String... path);

    T get(String id, String... path);

    CompletableFuture<T> put(String id, T value, String... path);

    CompletableFuture<Boolean> delete(String id, String... path);

    List<Identifier> identifiers(String... path);

    boolean has(Identifier identifier);

    T get(Identifier identifier);

    CompletableFuture<T> put(Identifier identifier, T value);
}
