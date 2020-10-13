/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app.entities;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ObjectArrays;
import de.ii.xtraplatform.dropwizard.domain.Jackson;
import de.ii.xtraplatform.runtime.domain.Logging;
import de.ii.xtraplatform.store.app.EventSourcing;
import de.ii.xtraplatform.store.app.ValueDecoderBase;
import de.ii.xtraplatform.store.app.ValueDecoderEnvVarSubstitution;
import de.ii.xtraplatform.store.app.ValueDecoderWithBuilder;
import de.ii.xtraplatform.store.app.ValueEncodingJackson;
import de.ii.xtraplatform.store.domain.AbstractMergeableKeyValueStore;
import de.ii.xtraplatform.store.domain.EventStore;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.ImmutableIdentifier;
import de.ii.xtraplatform.store.domain.ImmutableMutationEvent;
import de.ii.xtraplatform.store.domain.KeyPathAlias;
import de.ii.xtraplatform.store.domain.MutationEvent;
import de.ii.xtraplatform.store.domain.ValueCache;
import de.ii.xtraplatform.store.domain.ValueEncoding;
import de.ii.xtraplatform.store.domain.entities.AutoEntity;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaultsStore;
import de.ii.xtraplatform.store.domain.entities.EntityDataOverridesPath;
import de.ii.xtraplatform.store.domain.entities.EntityDataStore;
import de.ii.xtraplatform.store.domain.entities.EntityFactory;
import de.ii.xtraplatform.store.domain.entities.EntityStoreDecorator;
import java.io.IOException;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** @author zahnen */
@Component(publicFactory = false)
@Provides
@Instantiate
public class EntityDataStoreImpl extends AbstractMergeableKeyValueStore<EntityData>
    implements EntityDataStore<EntityData> {

  private static final Logger LOGGER = LoggerFactory.getLogger(EntityDataStoreImpl.class);
  private static final List<String> EVENT_TYPES = ImmutableList.of("entities", "overrides");

  private final boolean isEventStoreReadOnly;
  private final EntityFactory entityFactory;
  private final Queue<Map.Entry<Identifier, EntityData>> additionalEvents;
  private final ValueEncodingJackson<EntityData> valueEncoding;
  private final ValueEncodingJackson<Map<String, Object>> valueEncodingMap;
  private final EventSourcing<EntityData> eventSourcing;
  private final EntityDataDefaultsStore defaultsStore;

  protected EntityDataStoreImpl(
      @Requires EventStore eventStore,
      @Requires Jackson jackson,
      @Requires EntityFactory entityFactory,
      @Requires EntityDataDefaultsStore defaultsStore) {
    this.isEventStoreReadOnly = eventStore.isReadOnly();
    this.entityFactory = entityFactory;
    this.additionalEvents = new ConcurrentLinkedQueue<>();
    this.valueEncoding = new ValueEncodingJackson<>(jackson);
    this.valueEncodingMap = new ValueEncodingJackson<>(jackson);
    this.eventSourcing =
        new EventSourcing<>(
            eventStore, EVENT_TYPES, valueEncoding, this::onStart, Optional.of(this::processEvent));
    this.defaultsStore = defaultsStore;

    valueEncoding.addDecoderPreProcessor(new ValueDecoderEnvVarSubstitution());
    valueEncoding.addDecoderMiddleware(
        new ValueDecoderWithBuilder<>(this::getBuilder, eventSourcing));
    valueEncoding.addDecoderMiddleware(
        new ValueDecoderEntitySubtype(this::getBuilder, eventSourcing));
    valueEncoding.addDecoderMiddleware(
        new ValueDecoderEntityDataMigration(
            eventSourcing, entityFactory, this::addAdditionalEvent));

    valueEncodingMap.addDecoderMiddleware(new ValueDecoderBase<>(identifier -> new LinkedHashMap<>(), new ValueCache<Map<String, Object>>() {
      @Override
      public boolean isInCache(Identifier identifier) {
        return false;
      }

      @Override
      public Map<String, Object> getFromCache(Identifier identifier) {
        return null;
      }
    }));
  }

  // TODO: it seems this is needed for correct order (defaults < entities)
  @Validate
  private void onVal() {
    // LOGGER.debug("VALID");
  }

  @Override
  public ValueEncoding<EntityData> getValueEncoding() {
    return valueEncoding;
  }

  @Override
  protected EventSourcing<EntityData> getEventSourcing() {
    return eventSourcing;
  }

  @Override
  protected Map<String, Object> modifyPatch(Map<String, Object> partialData) {
    if (Objects.nonNull(partialData) && !partialData.isEmpty()) {
      // use mutable copy of map to allow null values
      /*Map<String, Object> modified = Maps.newHashMap(partialData);
      modified.put("lastModified", Instant.now()
                                          .toEpochMilli());
      return modified;*/
      return ImmutableMap.<String, Object>builder()
          .putAll(partialData)
          .put("lastModified", Instant.now().toEpochMilli())
          .build();
    }

    return partialData;
  }

  // TODO: onEmit middleware
  private List<MutationEvent> processEvent(MutationEvent event) {

    if (valueEncoding.isEmpty(event.payload()) || !valueEncoding.isSupported(event.format())) {
      return ImmutableList.of();
    }

    if (!event.type().equals(EVENT_TYPES.get(1))) {
      return ImmutableList.of(event);
    }

    EntityDataOverridesPath overridesPath = EntityDataOverridesPath.from(event.identifier());

    Identifier cacheKey =
        ImmutableIdentifier.builder()
            .addPath(overridesPath.getEntityType())
            .id(overridesPath.getEntityId())
            .build();

    ImmutableMutationEvent.Builder builder =
        ImmutableMutationEvent.builder().from(event).identifier(cacheKey);
    if (!overridesPath.getKeyPath().isEmpty()) {
      Optional<KeyPathAlias> keyPathAlias =
          entityFactory.getKeyPathAlias(
              overridesPath.getKeyPath().get(overridesPath.getKeyPath().size() - 1));
      try {
        byte[] nestedPayload =
            valueEncoding.nestPayload(
                event.payload(), event.format(), overridesPath.getKeyPath(), keyPathAlias);
        builder.payload(nestedPayload);
      } catch (IOException e) {
        LOGGER.error("Error:", e);
      }
    }

    return ImmutableList.of(builder.build());
  }

  protected EntityDataBuilder<EntityData> getBuilder(Identifier identifier) {
    return entityFactory.getDataBuilder(identifier.path().get(0), Optional.empty());
  }

  protected EntityDataBuilder<EntityData> getBuilder(Identifier identifier, String entitySubtype) {
    List<String> subtypePath = entityFactory.getTypeAsList(entitySubtype);

    ImmutableIdentifier defaultsIdentifier =
        ImmutableIdentifier.builder()
            .from(identifier)
            .id(EntityDataDefaultsStore.EVENT_TYPE)
            .addAllPath(subtypePath)
            .build();
    if (defaultsStore.has(defaultsIdentifier)) {
      return defaultsStore.getBuilder(defaultsIdentifier);
    }

    return entityFactory.getDataBuilder(identifier.path().get(0), Optional.of(entitySubtype));
  }

  protected EntityData hydrate(Identifier identifier, EntityData entityData) {
    String entityType = identifier.path().get(0);
    return entityFactory.hydrateData(identifier, entityType, entityData);
  }

  protected void addAdditionalEvent(Identifier identifier, EntityData entityData) {
    additionalEvents.add(new AbstractMap.SimpleImmutableEntry<>(identifier, entityData));
  }

  @Override
  protected CompletableFuture<Void> onStart() {
    // TODO: getAllPaths
    return playAdditionalEvents()
        .thenCompose(
            ignore -> {
              // second level migrations
              if (!additionalEvents.isEmpty()) {
                return playAdditionalEvents();
              }
              return CompletableFuture.completedFuture(null);
            })
        .thenCompose(
            ignore ->
                identifiers().stream()
                    // TODO: set priority per entity type (for now alphabetic works: codelists <
                    // providers < services)
                    .sorted(Comparator.comparing(identifier -> identifier.path().get(0)))
                    .reduce(
                        CompletableFuture.completedFuture((Void) null),
                        (completableFuture, identifier) ->
                            completableFuture.thenCompose(
                                ignore2 -> onCreate(identifier, get(identifier))),
                        (first, second) -> first.thenCompose(ignore2 -> second)))
        .thenCompose(entity -> CompletableFuture.completedFuture(null));
  }

  private CompletableFuture<EntityData> playAdditionalEvents() {
    CompletableFuture<EntityData> completableFuture = CompletableFuture.completedFuture(null);

    while (!additionalEvents.isEmpty()) {
      Map.Entry<Identifier, EntityData> entry = additionalEvents.remove();

      // TODO: which eventType?
      completableFuture =
          completableFuture.thenCompose(
              ignore -> {
                if (isEventStoreReadOnly) {
                  getEventSourcing()
                      .onEmit(
                          ImmutableMutationEvent.builder()
                              .type(EVENT_TYPES.get(0))
                              .identifier(entry.getKey())
                              .payload(valueEncoding.serialize(entry.getValue()))
                              .format(valueEncoding.getDefaultFormat().toString())
                              .build());
                  return CompletableFuture.completedFuture((EntityData) null);
                } else {
                  return dropWithoutTrigger(entry.getKey())
                      .thenCompose(
                          (deleted) -> putWithoutTrigger(entry.getKey(), entry.getValue()));
                }
              });
    }

    return completableFuture;
  }

  @Override
  protected CompletableFuture<Void> onCreate(Identifier identifier, EntityData entityData) {
    try(MDC.MDCCloseable closeable = Logging.putCloseable(Logging.CONTEXT.SERVICE, identifier.id())) {
      EntityData hydratedData = entityData;

      if (entityData instanceof AutoEntity) {
        AutoEntity autoEntity = (AutoEntity) entityData;
        if (autoEntity.isAuto() && autoEntity.isAutoPersist()) {
          hydratedData = hydrate(identifier, hydratedData);

          if (!isEventStoreReadOnly) {
            Map<String, Object> map = valueEncodingMap
                    .deserialize(identifier, valueEncoding.serialize(hydratedData), valueEncoding.getDefaultFormat());

            Map<String, Object> withoutDefaults = defaultsStore
                    .subtractDefaults(identifier, entityData.getEntitySubType(), map);

            putPartialWithoutTrigger(identifier, withoutDefaults).join();
            LOGGER.info(
                    "Entity of type '{}' with id '{}' is in autoPersist mode, generated configuration was saved.",
                    identifier.path()
                              .get(0),
                    entityData.getId());
          } else {
            LOGGER.warn("Entity of type '{}' with id '{}' is in autoPersist mode, but was not persisted because the store is read only.",
                    identifier.path()
                              .get(0),
                    entityData.getId());
          }
        }
      }

      hydratedData = hydrate(identifier, hydratedData);

      return entityFactory
              .createInstance(identifier.path()
                                        .get(0), identifier.id(), hydratedData)
              .whenComplete((entity, throwable) -> LOGGER.debug("Entity created: {}", identifier))
              .thenAccept(ignore -> CompletableFuture.completedFuture(null));
    }
  }

  @Override
  protected void onUpdate(Identifier identifier, EntityData entityData) {
    entityFactory.updateInstance(identifier.path().get(0), identifier.id(), entityData);
  }

  @Override
  protected void onDelete(Identifier identifier) {
    entityFactory.deleteInstance(identifier.path().get(0), identifier.id());
  }

  @Override
  protected void onFailure(Identifier identifier, Throwable throwable) {}

  @Override
  public <U extends EntityData> EntityDataStore<U> forType(Class<U> type) {
    final String typeCollectionName =
        entityFactory.getDataTypeName(type); // type.getSimpleName() + "s";
    return new EntityStoreDecorator<EntityData, U>() {
      @Override
      public EntityDataStore<EntityData> getDecorated() {
        return EntityDataStoreImpl.this;
      }

      @Override
      public String[] transformPath(String... path) {
        return ObjectArrays.concat(typeCollectionName, path);
      }
    };
  }
}
