package de.ii.xtraplatform.entity.api;

/**
 * @author zahnen
 */
public interface PersistentEntity {

    default String getId() {
        return getData() != null ? getData().getId() : null;
    }

    String getType();

    EntityData getData();
}
