package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.entity.api.EntityData;

import java.util.Map;
import java.util.OptionalLong;

public interface EntityFactory {

    EntityDataBuilder<EntityData> getDataBuilder(String entityType);

    EntityDataBuilder<EntityData> getDataBuilder(String entityType, long entitySchemaVersion);

    Map<Identifier, EntityData> migrateSchema(Identifier identifier, String entityType,
                                              EntityData entityData, OptionalLong targetVersion);

    String getDataTypeName(Class<? extends EntityData> entityDataClass);

    void createInstance(String entityType, String id, EntityData entityData);

    void updateInstance(String entityType, String id, EntityData entityData);

    void deleteInstance(String entityType, String id);
}
