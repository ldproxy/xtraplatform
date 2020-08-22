package de.ii.xtraplatform.store.domain.entities;

public interface EntityHydrator<T extends EntityData> {

    default T hydrateData(T data) {
        return data;
    }
}
