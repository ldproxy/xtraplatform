package de.ii.xtraplatform.entities.domain;

public interface EntityHydrator<T extends EntityData> {

    default T hydrateData(T data) {
        return data;
    }
}
