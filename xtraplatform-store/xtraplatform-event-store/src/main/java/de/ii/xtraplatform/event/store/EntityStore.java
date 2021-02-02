/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.event.store;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ObjectArrays;
import de.ii.xtraplatform.dropwizard.api.Jackson;
import de.ii.xtraplatform.entity.api.AutoEntity;
import de.ii.xtraplatform.entity.api.EntityData;
import de.ii.xtraplatform.entity.api.PersistentEntity;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author zahnen
 */

@Component(publicFactory = false)
@Provides
@Instantiate
public class EntityStore extends AbstractMergeableKeyValueStore<EntityData> implements EntityDataStore<EntityData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityStore.class);
    private static final List<String> EVENT_TYPES = ImmutableList.of("entities", "overrides");

    private final boolean isEventStoreReadOnly;
    private final EntityFactory entityFactory;
    private final Queue<Map.Entry<Identifier, EntityData>> additionalEvents;
    private final ValueEncodingJackson<EntityData> valueEncoding;
    private final EventSourcing<EntityData> eventSourcing;
    private final EntityDataDefaultsStore defaultsStore;

    protected EntityStore(@Requires EventStore eventStore, @Requires Jackson jackson,
                          @Requires EntityFactory entityFactory, @Requires EntityDataDefaultsStore defaultsStore) {
        this.isEventStoreReadOnly = eventStore.isReadOnly();
        this.entityFactory = entityFactory;
        this.additionalEvents = new ConcurrentLinkedQueue<>();
        this.valueEncoding = new ValueEncodingJackson<>(jackson);
        this.eventSourcing = new EventSourcing<>(eventStore, EVENT_TYPES, valueEncoding, this::onStart, Optional.of(this::processEvent));
        this.defaultsStore = defaultsStore;

        valueEncoding.addDecoderPreProcessor(new ValueDecoderEnvVarSubstitution());
        valueEncoding.addDecoderMiddleware(new ValueDecoderWithBuilder<>(this::getBuilder, eventSourcing));
        valueEncoding.addDecoderMiddleware(new ValueDecoderEntitySubtype(this::getBuilder, eventSourcing));
        valueEncoding.addDecoderMiddleware(new ValueDecoderEntityDataMigration(eventSourcing, entityFactory, this::addAdditionalEvent));
    }

    //TODO: it seems this is needed for correct order (defaults < entities)
    @Validate
    private void onVal() {
        //LOGGER.debug("VALID");
    }

    @Override
    protected ValueEncoding<EntityData> getValueEncoding() {
        return valueEncoding;
    }

    @Override
    protected EventSourcing<EntityData> getEventSourcing() {
        return eventSourcing;
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

    //TODO: onEmit middleware
    private List<MutationEvent> processEvent(MutationEvent event) {

        if (valueEncoding.isEmpty(event.payload())) {
            return ImmutableList.of();
        }

        if (!event.type()
                  .equals(EVENT_TYPES.get(1))) {
            return ImmutableList.of(event);
        }

        EntityDataOverridesPath overridesPath = EntityDataOverridesPath.from(event.identifier());

        Identifier cacheKey = ImmutableIdentifier.builder()
                                                 .addPath(overridesPath.getEntityType())
                                                 .id(overridesPath.getEntityId())
                                                 .build();

        ImmutableMutationEvent.Builder builder = ImmutableMutationEvent.builder()
                                                                       .from(event)
                                                                       .identifier(cacheKey);
        if (!overridesPath.getKeyPath()
                          .isEmpty()) {
            Optional<EntityDataDefaults.KeyPathAlias> keyPathAlias = entityFactory.getKeyPathAlias(overridesPath.getKeyPath()
                                                                                                                .get(overridesPath.getKeyPath()
                                                                                                                                  .size() - 1));
            try {
                byte[] nestedPayload = valueEncoding.nestPayload(event.payload(), event.format(), overridesPath.getKeyPath(), keyPathAlias);
                builder.payload(nestedPayload);
            } catch (IOException e) {
                LOGGER.error("Error:", e);
            }
        }

        return ImmutableList.of(builder.build());
    }

    protected EntityDataBuilder<EntityData> getBuilder(Identifier identifier) {
        return entityFactory.getDataBuilder(identifier.path()
                                                      .get(0), Optional.empty());
    }

    protected EntityDataBuilder<EntityData> getBuilder(Identifier identifier, String entitySubtype) {
        List<String> subtypePath = entityFactory.getTypeAsList(entitySubtype);

        ImmutableIdentifier defaultsIdentifier = ImmutableIdentifier.builder()
                                                                    .from(identifier)
                                                                    .id(EntityDataDefaultsStore.EVENT_TYPE)
                                                                    .addAllPath(subtypePath)
                                                                    .build();
        if (defaultsStore.has(defaultsIdentifier)) {
            return defaultsStore.getBuilder(defaultsIdentifier);
        }

        return entityFactory.getDataBuilder(identifier.path()
                                                      .get(0), Optional.of(entitySubtype));
    }

    protected EntityData hydrate(Identifier identifier, EntityData entityData) {
        String entityType = identifier.path()
                                      .get(0);
        return entityFactory.hydrateData(identifier, entityType, entityData);
    }

    protected void addAdditionalEvent(Identifier identifier, EntityData entityData) {
        additionalEvents.add(new AbstractMap.SimpleImmutableEntry<>(identifier, entityData));
    }

    @Override
    protected CompletableFuture<Void> onStart() {
        //TODO: getAllPaths
        return playAdditionalEvents().thenCompose(ignore -> {
            // second level migrations
            if (!additionalEvents.isEmpty()) {
                return playAdditionalEvents();
            }
            return CompletableFuture.completedFuture(null);
        })
                                     .thenCompose(ignore -> identifiers().stream()
                                                                         //TODO: set priority per entity type (for now alphabetic works: codelists < providers < services)
                                                                         .sorted(Comparator.comparing(identifier -> identifier.path()
                                                                                                                              .get(0)))
                                                                         .reduce(
                                                                                 CompletableFuture.completedFuture((PersistentEntity) null),
                                                                                 (completableFuture, identifier) -> completableFuture.thenCompose(ignore2 -> onCreate(identifier, get(identifier))),
                                                                                 (first, second) -> first.thenCompose(ignore2 -> second)
                                                                         ))
                                     .thenCompose(entity -> CompletableFuture.completedFuture(null));
    }

    private CompletableFuture<EntityData> playAdditionalEvents() {
        CompletableFuture<EntityData> completableFuture = CompletableFuture.completedFuture(null);

        while (!additionalEvents.isEmpty()) {
            Map.Entry<Identifier, EntityData> entry = additionalEvents.remove();

            //TODO: which eventType?
            completableFuture = completableFuture.thenCompose(ignore -> {
                if (isEventStoreReadOnly) {
                    getEventSourcing().onEmit(ImmutableMutationEvent.builder()
                                                                    .type(EVENT_TYPES.get(0))
                                                                    .identifier(entry.getKey())
                                                                    .payload(valueEncoding.serialize(entry.getValue()))
                                                                    .format(valueEncoding.getDefaultFormat()
                                                                                         .toString())
                                                                    .build());
                    return CompletableFuture.completedFuture((EntityData) null);
                } else {
                    return dropWithoutTrigger(entry.getKey()).thenCompose((deleted) -> putWithoutTrigger(entry.getKey(), entry.getValue()));
                }
            });
        }

        return completableFuture;
    }

    @Override
    protected CompletableFuture<PersistentEntity> onCreate(Identifier identifier, EntityData entityData) {
        EntityData hydratedData = hydrate(identifier, entityData);

        if (entityData instanceof AutoEntity) {
            AutoEntity autoEntity = (AutoEntity) entityData;
            if (autoEntity.isAuto() && autoEntity.isAutoPersist()) {
                putWithoutTrigger(identifier, hydratedData).join();
                LOGGER.info("Entity of type '{}' with id '{}' is in autoPersist mode, generated configuration was saved.", identifier.path()
                                                                                                                                     .get(0), entityData.getId());
            }
        }

        return entityFactory.createInstance(identifier.path()
                                                      .get(0), identifier.id(), hydratedData)
                            .whenComplete((entity, throwable) -> LOGGER.debug("Entity created: {}", identifier));
    }

    @Override
    protected void onUpdate(Identifier identifier, EntityData entityData) {
        EntityData hydratedData = hydrate(identifier, entityData);

        entityFactory.updateInstance(identifier.path()
                                               .get(0), identifier.id(), hydratedData);
    }

    @Override
    protected void onDelete(Identifier identifier) {
        entityFactory.deleteInstance(identifier.path()
                                               .get(0), identifier.id());
    }

    @Override
    protected void onFailure(Identifier identifier, Throwable throwable) {

    }

    @Override
    public <U extends EntityData> EntityDataStore<U> forType(Class<U> type) {
        final String typeCollectionName = entityFactory.getDataTypeName(type);// type.getSimpleName() + "s";
        return new EntityStoreDecorator<EntityData, U>() {
            @Override
            public EntityDataStore<EntityData> getDecorated() {
                return EntityStore.this;
            }

            @Override
            public String[] transformPath(String... path) {
                return ObjectArrays.concat(typeCollectionName, path);
            }
        };
    }
}
