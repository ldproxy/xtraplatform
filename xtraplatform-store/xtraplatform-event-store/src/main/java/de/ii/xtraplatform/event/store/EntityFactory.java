package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.entity.api.EntityData;
import de.ii.xtraplatform.entity.api.PersistentEntity;

import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

public interface EntityFactory {

    EntityDataBuilder<EntityData> getDataBuilder(String entityType);

    EntityDataBuilder<EntityData> getDataBuilder(String entityType, long entitySchemaVersion);

    Map<Identifier, EntityData> migrateSchema(Identifier identifier, String entityType,
                                              EntityData entityData, OptionalLong targetVersion);

    String getDataTypeName(Class<? extends EntityData> entityDataClass);

    CompletableFuture<PersistentEntity> createInstance(String entityType, String id, EntityData entityData);

    CompletableFuture<PersistentEntity> updateInstance(String entityType, String id, EntityData entityData);

    void deleteInstance(String entityType, String id);
}