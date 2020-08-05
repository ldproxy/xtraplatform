package de.ii.xtraplatform.event.store;

public interface ValueCache<T> {
    boolean isInCache(Identifier identifier);

    T getFromCache(Identifier identifier);
}
