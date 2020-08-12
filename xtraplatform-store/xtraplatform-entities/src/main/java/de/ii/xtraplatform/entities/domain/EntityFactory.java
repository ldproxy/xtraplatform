package de.ii.xtraplatform.entities.domain;

import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.KeyPathAlias;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

public interface EntityFactory {

    EntityDataBuilder<EntityData> getDataBuilder(String entityType, Optional<String> entitySubType);

    Optional<KeyPathAlias> getKeyPathAlias(String keyPath);

    List<List<String>> getSubTypes(String entityType, List<String> entitySubType);

    EntityDataBuilder<EntityData> getDataBuilders(String entityType, long entitySchemaVersion, Optional<String> entitySubType);

    Optional<String> getTypeAsString(List<String> entitySubtype);

    Map<Identifier, EntityData> migrateSchema(Identifier identifier, String entityType,
                                              EntityData entityData, Optional<String> entitySubType, OptionalLong targetVersion);

    EntityData hydrateData(Identifier identifier, String entityType, EntityData entityData);

    String getDataTypeName(Class<? extends EntityData> entityDataClass);

    CompletableFuture<PersistentEntity> createInstance(String entityType, String id, EntityData entityData);

    CompletableFuture<PersistentEntity> updateInstance(String entityType, String id, EntityData entityData);

    void deleteInstance(String entityType, String id);

    List<String> getTypeAsList(String entitySubtype);
}
