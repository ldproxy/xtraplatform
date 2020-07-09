package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.entity.api.Mergeable;

public interface MergeableBuilder<T extends Mergeable> {
    T build();
    MergeableBuilder<T> from(T mergeable);
}
