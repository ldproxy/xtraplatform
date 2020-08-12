package de.ii.xtraplatform.store.domain;

public interface Builder<T extends Value> {
    T build();

    Builder<T> from(Value value);
}
