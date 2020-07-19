package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.entity.api.Value;

public interface Builder<T extends Value> {
    T build();

    Builder<T> from(Value value);
}
