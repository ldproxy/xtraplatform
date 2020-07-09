package de.ii.xtraplatform.event.store;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.entity.api.Mergeable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static de.ii.xtraplatform.event.store.EventSourcingTests.DESERIALIZE_API_BUILDINGBLOCK_MIGRATION;
import static de.ii.xtraplatform.event.store.EventSourcingTests.DESERIALIZE_IMMUTABLE_BUILDER_NESTED;
import static de.ii.xtraplatform.event.store.EventSourcingTests.DESERIALIZE_MERGEABLE_MAP_BUILDER_WRAPPER;

public abstract class AbstractMergeableKeyValueStore<T extends Mergeable, U extends MergeableBuilder<T>> extends AbstractKeyValueStore<T> implements MergeableKeyValueStore<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMergeableKeyValueStore.class);
    private static final byte[] JSON_NULL = "null".getBytes();
    private static final byte[] YAML_NULL = "--- null\n".getBytes();
    enum FORMAT {JSON, YML, YAML, ION}
    static final FORMAT DEFAULT_FORMAT = FORMAT.YML;

    // TODO: use smile/ion mapper for distributed store
    final Map<FORMAT, ObjectMapper> mappers;

    protected AbstractMergeableKeyValueStore(EventStore eventStore, String eventType, ObjectMapper baseMapper, ObjectMapper newYamlMapper) {
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

    protected abstract U getBuilder(Identifier identifier);

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
            // default for deserialization is JSON because old configuration files without file extension are JSON
            payloadFormat = Objects.nonNull(format) ? FORMAT.valueOf(format.toUpperCase()) : FORMAT.JSON;
        } catch (Throwable e) {
            LOGGER.error("Could not deserialize entity {}, format '{}' unknown.", identifier, format);
            return null;
        }

        T mergeable = null;

        try {
            U builder = getBuilder(identifier);

            mergeable = deserialize(builder, identifier, payload, payloadFormat);

            mergeable = postProcess(identifier, mergeable, payload, payloadFormat);

        } catch (Throwable e) {
            try {
                mergeable = tryAlternativeDeserialization(identifier, payload, payloadFormat);
            } catch (Throwable e2) {
                LOGGER.error("Deserialization error: {}", e.getMessage());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Stacktrace:", e);
                }
            }
        }

        return mergeable;
    }

    protected T postProcess(Identifier identifier, T mergeable, byte[] payload, FORMAT payloadFormat) throws IOException {
        return mergeable;
    }

    protected T tryAlternativeDeserialization(Identifier identifier, byte[] payload, FORMAT payloadFormat) throws IOException {
        throw new IllegalStateException();
    }


    protected T deserialize(U builder, Identifier identifier, byte[] payload, FORMAT format) throws IOException {
        if (eventSourcing.isInCache(identifier)) {
            builder.from(eventSourcing.getFromCache(identifier));
        }

        mappers.get(format).readerForUpdating(builder)
                  .readValue(payload);

        return builder.build();
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
        return eventSourcing.isInCache(identifier) && Objects.nonNull(deserialize(identifier, payload, DEFAULT_FORMAT.toString()));
    }

    @Override
    public CompletableFuture<T> patch(String id, Map<String, Object> partialData, String... path) {

        final Identifier identifier = Identifier.from(id, path);

        byte[] payload = serialize(modifyPatch(partialData));

        //validate
        if (!isUpdateValid(identifier, payload)) {
            throw new IllegalArgumentException("Partial update for ... not valid");
        }

        //TODO: SnapshotProvider???
        byte[] merged = serialize(deserialize(identifier, payload, DEFAULT_FORMAT.toString()));

        return eventSourcing.pushMutationEventRaw(identifier, merged)
                            .whenComplete((entityData, throwable) -> {
                                if (Objects.nonNull(entityData)) {
                                    onUpdate(identifier, entityData);
                                } else if (Objects.nonNull(throwable)) {
                                    onFailure(identifier, throwable);
                                }
                            });
    }

    protected Map<String, Object> modifyPatch(Map<String, Object> partialData) {
        return partialData;
    }
}
