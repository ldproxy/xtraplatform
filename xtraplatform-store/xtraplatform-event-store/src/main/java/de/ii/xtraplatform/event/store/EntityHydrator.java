package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.entity.api.EntityData;

public interface EntityHydrator<T extends EntityData> {

    default T hydrateData(T data) {
        return data;
    }
}
