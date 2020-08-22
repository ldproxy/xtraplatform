package de.ii.xtraplatform.store.domain.entities;

import de.ii.xtraplatform.store.domain.Builder;
import de.ii.xtraplatform.store.domain.Value;

public interface EntityDataBuilder<T extends EntityData> extends Builder<T> {

    @Override
    T build();

    @Override
    EntityDataBuilder<T> from(Value value);

    EntityDataBuilder<T> from(EntityData value);
}
