package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.entity.api.EntityData;
import de.ii.xtraplatform.entity.api.Value;

public interface EntityDataBuilder<T extends EntityData> extends Builder<T> {

    @Override
    T build();

    @Override
    EntityDataBuilder<T> from(Value value);

    EntityDataBuilder<T> from(EntityData value);
}
