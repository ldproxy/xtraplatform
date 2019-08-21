package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.entity.api.EntityData;

public interface EntityDataBuilder<T extends EntityData> {
    T build();
    EntityDataBuilder<T> from(EntityData data);
}
