package de.ii.xtraplatform.entity.api;

/**
 * @author zahnen
 */
public interface PersistentEntity {

    default String getId() {
        return getData() != null ? getData().getId() : null;
    }

    EntityData getData();
}
