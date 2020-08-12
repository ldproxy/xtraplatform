package de.ii.xtraplatform.store.domain;

public interface ValueCache<T> {
    boolean isInCache(Identifier identifier);

    T getFromCache(Identifier identifier);
}
