package de.ii.xtraplatform.event.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.entity.api.EntityData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

//TODO: AbstractMergeableImmutableStore
public abstract class AbstractEntityDataStore<T extends EntityData, U extends EntityDataBuilder<T>> extends AbstractMergeableKeyValueStore<T,U> implements EntityDataStore<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEntityDataStore.class);


    protected AbstractEntityDataStore(EventStore eventStore, String eventType, ObjectMapper baseMapper, ObjectMapper newYamlMapper) {
        super(eventStore, eventType, baseMapper, newYamlMapper);
    }

    protected abstract U getBuilder(Identifier identifier, long entitySchemaVersion, Optional<String> entitySubType);

    protected abstract Map<Identifier, T> migrate(Identifier identifier, T entityData, Optional<String> entitySubType, OptionalLong targetVersion);

    protected abstract EntityData hydrate(Identifier identifier, EntityData entityData);

    protected abstract void addAdditionalEvent(Identifier identifier, T entityData);

    @Override
    protected T postProcess(Identifier identifier, T entityData, byte[] payload, FORMAT format) throws IOException {
        if (entityData.getEntityStorageVersion() < entityData.getEntitySchemaVersion()) {
            migrateSchema(identifier, payload, format, entityData.getEntityStorageVersion(), entityData.getEntitySubType(), OptionalLong.of(entityData.getEntitySchemaVersion()));

            return null;
        }

        return entityData;
    }

    @Override
    protected T tryAlternativeDeserialization(Identifier identifier, byte[] payload, FORMAT format) throws IOException {
        TypeReference<LinkedHashMap<String, Object>> typeRef = new TypeReference<LinkedHashMap<String, Object>>() {};
        Map<String, Object> map = mappers.get(format).readValue(payload, typeRef);

        if (!map.containsKey("id")) {
            throw new IllegalArgumentException("not a valid entity, no id found");
        }

        long storageVersion =  ((Number) map.getOrDefault("entityStorageVersion", 1)).longValue();

        migrateSchema(identifier, payload, format, storageVersion, Optional.empty(), OptionalLong.empty());

        return null;
    }

    private void migrateSchema(Identifier identifier, byte[] payload,
                               FORMAT format,
                               long storageVersion, Optional<String> entitySubType, OptionalLong targetVersion) throws IOException {
        U builder = getBuilder(identifier, storageVersion, entitySubType);
        T entityDataOld = deserialize(builder, identifier, payload, format);

        Map<Identifier, T> entityDataNew = migrate(identifier, entityDataOld, entitySubType, targetVersion);

        entityDataNew.forEach(this::addAdditionalEvent);

    }

    @Override
    protected Map<String, Object> modifyPatch(Map<String, Object> partialData) {
        if (Objects.nonNull(partialData) && !partialData.isEmpty()) {
            //use mutable copy of map to allow null values
            /*Map<String, Object> modified = Maps.newHashMap(partialData);
            modified.put("lastModified", Instant.now()
                                                .toEpochMilli());
            return modified;*/
            return ImmutableMap.<String, Object>builder()
                    .putAll(partialData)
                    .put("lastModified", Instant.now()
                                                .toEpochMilli())
                    .build();
        }

        return partialData;
    }
}
