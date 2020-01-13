package de.ii.xtraplatform.event.store;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.entity.api.EntityData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

import static de.ii.xtraplatform.event.store.EventSourcingTests.DESERIALIZE_API_BUILDINGBLOCK_MIGRATION;
import static de.ii.xtraplatform.event.store.EventSourcingTests.DESERIALIZE_MERGEABLE_MAP_BUILDER_WRAPPER;
import static de.ii.xtraplatform.event.store.EventSourcingTests.DESERIALIZE_IMMUTABLE_BUILDER_NESTED;

//TODO: AbstractMergeableImmutableStore
public abstract class AbstractEntityDataStore<T extends EntityData> extends AbstractKeyValueStore<T> implements EntityDataStore<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEntityDataStore.class);
    private static final byte[] JSON_NULL = "null".getBytes();
    private static final byte[] YAML_NULL = "--- null\n".getBytes();
    enum FORMAT {JSON, YML, YAML, ION}
    static final FORMAT DEFAULT_FORMAT = FORMAT.YML;

    // TODO: use smile/ion mapper for distributed store
    private final Map<FORMAT, ObjectMapper> mappers;

    protected AbstractEntityDataStore(EventStore eventStore, String eventType, ObjectMapper baseMapper, ObjectMapper newYamlMapper) {
        super(eventStore, eventType);

        ObjectMapper jsonMapper = baseMapper.copy()
                                    .registerModule(DESERIALIZE_IMMUTABLE_BUILDER_NESTED)
                                    .registerModule(DESERIALIZE_MERGEABLE_MAP_BUILDER_WRAPPER)
                                    .registerModule(DESERIALIZE_API_BUILDINGBLOCK_MIGRATION)
                                    .setDefaultMergeable(true);

        ObjectMapper yamlMapper = newYamlMapper.registerModule(DESERIALIZE_IMMUTABLE_BUILDER_NESTED)
                                               .registerModule(DESERIALIZE_MERGEABLE_MAP_BUILDER_WRAPPER)
                                               .registerModule(DESERIALIZE_API_BUILDINGBLOCK_MIGRATION)
                                               .setDefaultMergeable(true)
                                               .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        this.mappers = ImmutableMap.of(
          FORMAT.JSON, jsonMapper,
          FORMAT.YML, yamlMapper,
          FORMAT.YAML, yamlMapper
        );
    }

    protected abstract EntityDataBuilder<T> getBuilder(Identifier identifier);

    protected abstract EntityDataBuilder<T> getBuilder(Identifier identifier, long entitySchemaVersion);

    protected abstract Map<Identifier, T> migrate(Identifier identifier, T entityData, OptionalLong targetVersion);

    protected abstract void addAdditionalEvent(Identifier identifier, T entityData);

    @Override
    protected String getDefaultFormat() {
        return DEFAULT_FORMAT.name();
    }

    @Override
    protected T deserialize(Identifier identifier, byte[] payload, String format) {
        // "null" as payload means delete
        if (Arrays.equals(payload, JSON_NULL) || Arrays.equals(payload, YAML_NULL)) {
            return null;
        }

        FORMAT payloadFormat;
        try {
            payloadFormat = Objects.nonNull(format) ? FORMAT.valueOf(format.toUpperCase()) : FORMAT.JSON;
        } catch (Throwable e) {
            LOGGER.error("Could not deserialize entity {}, format '{}' unknown.", identifier, format);
            return null;
        }

        T entityData = null;

        try {
            EntityDataBuilder<T> builder = getBuilder(identifier);

            entityData = deserialize(builder, identifier, payload, payloadFormat);


            //TODO: migrate overrides in ReadWriteStore

            //TODO: remove original file on migration, backup

            entityData = ensureFreshestSchema(identifier, entityData, payload, payloadFormat);

        } catch (Throwable e) {
            try {
                entityData = tryMigrateSchema(identifier, payload, payloadFormat);
            } catch (Throwable e2) {
                LOGGER.error("Deserialization error: {}", e.getMessage());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Stacktrace:", e);
                }
            }
        }

        return entityData;
    }

    private T deserialize(EntityDataBuilder<T> builder, Identifier identifier, byte[] payload, FORMAT format) throws IOException {
        if (eventSourcing.isInCache(identifier)) {
            builder.from(eventSourcing.getFromCache(identifier));
        }

        mappers.get(format).readerForUpdating(builder)
                  .readValue(payload);

        return builder.build();
    }

    private T ensureFreshestSchema(Identifier identifier, T entityData, byte[] payload, FORMAT format) throws IOException {
        if (entityData.getEntityStorageVersion() < entityData.getEntitySchemaVersion()) {
            migrateSchema(identifier, payload, format, entityData.getEntityStorageVersion(), OptionalLong.of(entityData.getEntitySchemaVersion()));

            return null;
        }

        return entityData;
    }

    private T tryMigrateSchema(Identifier identifier, byte[] payload, FORMAT format) throws IOException {
        TypeReference<LinkedHashMap<String, Object>> typeRef = new TypeReference<LinkedHashMap<String, Object>>() {};
        Map<String, Object> map = mappers.get(format).readValue(payload, typeRef);

        if (!map.containsKey("id")) {
            throw new IllegalArgumentException("not a valid entity, no id found");
        }

        long storageVersion =  ((Number) map.getOrDefault("entityStorageVersion", 1)).longValue();

        migrateSchema(identifier, payload, format, storageVersion, OptionalLong.empty());

        return null;
    }

    private void migrateSchema(Identifier identifier, byte[] payload,
                               FORMAT format,
                               long storageVersion, OptionalLong targetVersion) throws IOException {
        EntityDataBuilder<T> builder = getBuilder(identifier, storageVersion);
        T entityDataOld = deserialize(builder, identifier, payload, format);

        //TODO: return Map<Identifier, EntityData>, so we can include Provider
        Map<Identifier, T> entityDataNew = migrate(identifier, entityDataOld, targetVersion);

        entityDataNew.forEach(this::addAdditionalEvent);

    }

    //TODO: default serialization format should depend on EventStore implementation
    @Override
    protected byte[] serialize(T data) {
        try {
            return mappers.get(DEFAULT_FORMAT).writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            //should never happen
            throw new IllegalStateException("Unexpected serialization error", e);
        }
    }

    private byte[] serialize(Map<String, Object> data) {
        try {
            return mappers.get(DEFAULT_FORMAT).writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            //should never happen
            throw new IllegalStateException("Unexpected serialization error", e);
        }
    }

    //TODO: an in-progress event (e.g. drop) might invalidate this one, do we need distributed locks???
    private boolean isUpdateValid(Identifier identifier, byte[] payload) {
        return eventSourcing.isInCache(identifier) && Objects.nonNull(deserialize(identifier, payload, null));
    }

    @Override
    public CompletableFuture<T> patch(String id, Map<String, Object> partialData, String... path) {

        final Identifier identifier = Identifier.from(id, path);

        byte[] payload = serialize(addLastModified(partialData));

        //validate
        if (!isUpdateValid(identifier, payload)) {
            throw new IllegalArgumentException("Partial update for ... not valid");
        }

        //TODO: SnapshotProvider???
        byte[] merged = serialize(deserialize(identifier, payload, null));

        return eventSourcing.pushMutationEventRaw(identifier, merged)
                            .whenComplete((entityData, throwable) -> {
                                if (Objects.nonNull(entityData)) {
                                    onUpdate(identifier, entityData);
                                } else if (Objects.nonNull(throwable)) {
                                    onFailure(identifier, throwable);
                                }
                            });
    }

    private Map<String, Object> addLastModified(Map<String, Object> partialData) {
        if (Objects.nonNull(partialData) && !partialData.isEmpty()) {
            return ImmutableMap.<String, Object>builder()
                    .putAll(partialData)
                    .put("lastModified", Instant.now()
                                                .toEpochMilli())
                    .build();
        }

        return partialData;
    }
}
