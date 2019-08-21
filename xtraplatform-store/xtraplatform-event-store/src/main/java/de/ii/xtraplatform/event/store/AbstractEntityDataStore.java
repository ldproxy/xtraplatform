package de.ii.xtraplatform.event.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.entity.api.EntityData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static de.ii.xtraplatform.event.store.EventSourcingTests.DESERIALIZE_MERGEABLE_MAP_BUILDER_WRAPPER;
import static de.ii.xtraplatform.event.store.EventSourcingTests.DESERIALIZE_IMMUTABLE_BUILDER_NESTED;

//TODO: AbstractMergeableImmutableStore
public abstract class AbstractEntityDataStore<T extends EntityData> extends AbstractKeyValueStore<T> implements EntityDataStore<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEntityDataStore.class);
    private static final byte[] JSON_NULL = "null".getBytes();

    //TODO: use Jackson.newMapper or configure explicitely,
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            //TODO: needed even if setters are never used
            .registerModule(DESERIALIZE_IMMUTABLE_BUILDER_NESTED)
            .registerModule(DESERIALIZE_MERGEABLE_MAP_BUILDER_WRAPPER)
            .setDefaultMergeable(true);

    // TODO: use smile/ion factory
    private final ObjectMapper ION_MAPPER;

    protected AbstractEntityDataStore(EventStore eventStore, String eventType) {
        this(eventStore, eventType, new ObjectMapper());
    }

    protected AbstractEntityDataStore(EventStore eventStore, String eventType, ObjectMapper baseMapper) {
        super(eventStore, eventType);

        this.ION_MAPPER = baseMapper.copy()
                                    .registerModule(DESERIALIZE_IMMUTABLE_BUILDER_NESTED)
                                    .registerModule(DESERIALIZE_MERGEABLE_MAP_BUILDER_WRAPPER)
                                    .setDefaultMergeable(true);
    }

    protected abstract EntityDataBuilder<T> getBuilder(Identifier identifier);

    @Override
    protected T deserialize(Identifier identifier, byte[] payload) {
        // "null" as payload means delete
        if (Arrays.equals(payload, JSON_NULL)) {
            return null;
        }

        try {
            EntityDataBuilder<T> builder = getBuilder(identifier);

            if (eventSourcing.isInCache(identifier)) {
                builder.from(eventSourcing.getFromCache(identifier));
            }

            ION_MAPPER.readerForUpdating(builder)
                      .readValue(payload);

            return builder.build();
        } catch (Throwable e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Deserialization error", e);
            }
            return null;
        }
    }

    @Override
    protected byte[] serialize(T data) {
        try {
            return ION_MAPPER.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            //should never happen
            throw new IllegalStateException("Unexpected serialization error", e);
        }
    }

    private byte[] serialize(Map<String, Object> data) {
        try {
            return ION_MAPPER.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            //should never happen
            throw new IllegalStateException("Unexpected serialization error", e);
        }
    }

    //TODO: an in-progress event (e.g. drop) might invalidate this one, do we need distributed locks???
    private boolean isUpdateValid(Identifier identifier, byte[] payload) {
        return eventSourcing.isInCache(identifier) && Objects.nonNull(deserialize(identifier, payload));
    }

    //TODO: to separate class EntityDataGenerator??? define generic for input params???
    //@Override
    public T generateEntity(Map<String, Object> data, String... path) throws IOException {
        return null;
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
        byte[] merged = serialize(deserialize(identifier, payload));

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
