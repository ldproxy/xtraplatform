package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.entity.api.EntityData;

public interface EntityFactory {
    EntityDataBuilder<EntityData> getDataBuilder(String entityType);

    String getDataTypeName(Class<? extends EntityData> entityDataClass);

    void createInstance(String entityType, String id, EntityData entityData);

    void updateInstance(String entityType, String id, EntityData entityData);

    void deleteInstance(String entityType, String id);
}
