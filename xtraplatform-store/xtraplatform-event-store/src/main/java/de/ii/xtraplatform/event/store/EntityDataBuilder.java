package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.Value;

public interface EntityDataBuilder<T extends EntityData> extends Builder<T> {

    @Override
    T build();

    @Override
    EntityDataBuilder<T> from(Value value);

    EntityDataBuilder<T> from(EntityData value);
}
