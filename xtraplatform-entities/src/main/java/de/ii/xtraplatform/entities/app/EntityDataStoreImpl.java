/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.ObjectArrays;
import dagger.Lazy;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.blobs.domain.ResourceStore;
import de.ii.xtraplatform.entities.domain.AbstractMergeableKeyValueStore;
import de.ii.xtraplatform.entities.domain.AutoEntity;
import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.EntityDataBuilder;
import de.ii.xtraplatform.entities.domain.EntityDataDefaultsStore;
import de.ii.xtraplatform.entities.domain.EntityDataOverridesPath;
import de.ii.xtraplatform.entities.domain.EntityDataStore;
import de.ii.xtraplatform.entities.domain.EntityFactoriesImpl;
import de.ii.xtraplatform.entities.domain.EntityFactory;
import de.ii.xtraplatform.entities.domain.EntityStoreDecorator;
import de.ii.xtraplatform.entities.domain.EventStore;
import de.ii.xtraplatform.entities.domain.ImmutableReplayEvent;
import de.ii.xtraplatform.entities.domain.KeyPathAlias;
import de.ii.xtraplatform.entities.domain.ReplayEvent;
import de.ii.xtraplatform.values.api.ValueDecoderBase;
import de.ii.xtraplatform.values.api.ValueDecoderEnvVarSubstitution;
import de.ii.xtraplatform.values.api.ValueDecoderWithBuilder;
import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.ImmutableIdentifier;
import de.ii.xtraplatform.values.domain.ValueCache;
import de.ii.xtraplatform.values.domain.ValueEncoding;
import de.ii.xtraplatform.values.domain.ValueStore;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * @author zahnen
 */
@Singleton
@AutoBind(interfaces = {EntityDataStore.class, AppLifeCycle.class})
public class EntityDataStoreImpl extends AbstractMergeableKeyValueStore<EntityData>
    implements EntityDataStore<EntityData>, AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(EntityDataStoreImpl.class);

  private final boolean isEventStoreReadOnly;
  private final EntityFactoriesImpl entityFactories;
  private final Queue<Map.Entry<Identifier, EntityData>> additionalEvents;
  private final ValueEncodingJacksonWithNesting<EntityData> valueEncoding;
  private final ValueEncodingJacksonWithNesting<Map<String, Object>> valueEncodingMap;
  private final EventSourcing<EntityData> eventSourcing;
  private final EntityDataDefaultsStore defaultsStore;
  private final Supplier<Void> blobStoreReady;
  private final Supplier<Void> valueStoreReady;
  private final boolean noDefaults;

  @Inject
  public EntityDataStoreImpl(
      AppContext appContext,
      EventStore eventStore,
      Jackson jackson,
      Lazy<Set<EntityFactory>> entityFactories,
      EntityDataDefaultsStore defaultsStore,
      ResourceStore blobStore,
      ValueStore valueStore) {
    this(
        appContext,
        eventStore,
        jackson,
        entityFactories,
        defaultsStore,
        blobStore,
        valueStore,
        false);
  }

  // for ldproxy-cfg
  public EntityDataStoreImpl(
      AppContext appContext,
      EventStore eventStore,
      Jackson jackson,
      Lazy<Set<EntityFactory>> entityFactories,
      EntityDataDefaultsStore defaultsStore,
      ResourceStore blobStore,
      ValueStore valueStore,
      boolean noDefaults) {
    this.isEventStoreReadOnly = eventStore.isReadOnly();
    this.entityFactories = new EntityFactoriesImpl(entityFactories);
    this.additionalEvents = new ConcurrentLinkedQueue<>();
    this.valueEncoding =
        new ValueEncodingJacksonWithNesting<>(
            jackson, appContext.getConfiguration().getStore().isFailOnUnknownProperties());
    this.valueEncodingMap =
        new ValueEncodingJacksonWithNesting<>(
            jackson, appContext.getConfiguration().getStore().isFailOnUnknownProperties());
    this.eventSourcing =
        new EventSourcing<>(
            eventStore,
            EntityDataStore.EVENT_TYPES,
            valueEncoding,
            this::onListenStart,
            Optional.of(this::processEvent),
            Optional.empty(),
            Optional.of(this::onUpdate));
    this.defaultsStore = defaultsStore;
    this.blobStoreReady = blobStore.onReady()::join;
    this.valueStoreReady = valueStore.onReady()::join;
    this.noDefaults = noDefaults;

    valueEncoding.addDecoderPreProcessor(new ValueDecoderEnvVarSubstitution());
    valueEncoding.addDecoderMiddleware(
        new ValueDecoderWithBuilder<>(this::getBuilder, eventSourcing));
    valueEncoding.addDecoderMiddleware(
        new ValueDecoderEntitySubtype(this::getBuilder, eventSourcing));
    /*TODO valueEncoding.addDecoderMiddleware(
    new ValueDecoderEntityDataMigration(
        eventSourcing, entityFactories, this::addAdditionalEvent));*/
    valueEncoding.addDecoderMiddleware(new ValueDecoderIdValidator());
    valueEncoding.addDecoderMiddleware(
        new ValueDecoderEntityPreHash(this::getBuilder, valueEncoding::hash));

    valueEncodingMap.addDecoderMiddleware(
        new ValueDecoderBase<>(
            identifier -> new LinkedHashMap<>(),
            new ValueCache<Map<String, Object>>() {
              @Override
              public boolean has(Identifier identifier) {
                return false;
              }

              @Override
              public boolean has(Predicate<Identifier> keyMatcher) {
                return false;
              }

              @Override
              public Map<String, Object> get(Identifier identifier) {
                return null;
              }
            }));
  }

  @Override
  public int getPriority() {
    return 40;
  }

  @Override
  public void onStart() {
    eventSourcing.start();
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
      Map<String, Object> modified = Maps.newHashMap(partialData);
      modified.put("lastModified", Instant.now().toEpochMilli());
      return modified;
      /*return ImmutableMap.<String, Object>builder()
      .putAll(partialData)
      .put("lastModified", Instant.now().toEpochMilli())
      .build();*/
    }

    return partialData;
  }

  // TODO: onEmit middleware
  private List<ReplayEvent> processEvent(ReplayEvent event) {

    if (valueEncoding.isEmpty(event.payload()) || !valueEncoding.isSupported(event.format())) {
      return ImmutableList.of();
    }

    if (!event.isDelete()
        && event.type().equals(EntityDataStore.EVENT_TYPE_ENTITIES)
        && eventSourcing.has(isDuplicate(event.identifier()))) {
      LOGGER.warn(
          "Ignoring entity '{}' from {} because it already exists. An entity can only exist in a single group.",
          event.asPathNoType(),
          event.source().orElse("UNKNOWN"));
      return ImmutableList.of();
    }

    if (!event.isDelete()
        && event.type().equals(EntityDataStore.EVENT_TYPE_ENTITIES)
        && eventSourcing.has(event.identifier())) {
      LOGGER.warn(
          "Ignoring entity '{}' from {} because it already exists. An entity can only exist in a single source, use overrides to update it from another source.",
          event.asPathNoType(),
          event.source().orElse("UNKNOWN"));
      return ImmutableList.of();
    }

    if (!event.type().equals(EntityDataStore.EVENT_TYPE_OVERRIDES)) {
      return ImmutableList.of(event);
    }

    EntityDataOverridesPath overridesPath =
        EntityDataOverridesPath.from(event.identifier(), entityFactories.getTypes());

    Identifier cacheKey = overridesPath.asIdentifier();

    // override without matching entity
    if (!eventSourcing.has(cacheKey)) {
      LOGGER.warn("Ignoring override '{}', no matching entity found", event.asPath());
      return ImmutableList.of();
    }

    ImmutableReplayEvent.Builder builder =
        ImmutableReplayEvent.builder().from(event).identifier(cacheKey);
    if (!overridesPath.getKeyPath().isEmpty()) {
      // TODO: multiple subtypes
      Optional<KeyPathAlias> keyPathAlias =
          entityFactories
              .get(overridesPath.getEntityType())
              .getKeyPathAlias(
                  overridesPath.getKeyPath().get(overridesPath.getKeyPath().size() - 1));
      try {
        byte[] nestedPayload =
            valueEncoding.nestPayload(
                event.payload(), event.format(), overridesPath.getKeyPath(), keyPathAlias);
        builder.payload(nestedPayload);
      } catch (IOException e) {
        LogContext.error(LOGGER, e, "Deserialization error");
      }
    }

    return ImmutableList.of(builder.build());
  }

  @Override
  public EntityDataBuilder<EntityData> getBuilder(
      Identifier identifier, Optional<String> entitySubtype) {
    return entitySubtype.isPresent()
        ? getBuilder(identifier, entitySubtype.get())
        : getBuilder(identifier);
  }

  protected EntityDataBuilder<EntityData> getBuilder(Identifier identifier) {
    if (noDefaults) {
      return (EntityDataBuilder<EntityData>)
          entityFactories.get(EntityDataStore.entityType(identifier)).emptySuperDataBuilder();
    }

    // TODO: try defaults like below?

    return (EntityDataBuilder<EntityData>)
        entityFactories.get(EntityDataStore.entityType(identifier)).superDataBuilder();
  }

  protected EntityDataBuilder<EntityData> getBuilder(Identifier identifier, String entitySubtype) {
    if (noDefaults) {
      return (EntityDataBuilder<EntityData>)
          entityFactories
              .get(EntityDataStore.entityType(identifier), entitySubtype)
              .emptyDataBuilder();
    }

    Identifier defaultsIdentifier = EntityDataStore.defaults(identifier, entitySubtype);

    if (defaultsStore.has(defaultsIdentifier)) {
      return defaultsStore.getBuilder(defaultsIdentifier);
    }

    for (int i = 1; i < defaultsIdentifier.path().size(); i++) {
      Identifier parent = parent(defaultsIdentifier, i);

      if (defaultsStore.has(parent)) {
        return defaultsStore.getBuilder(parent);
      }
    }

    return (EntityDataBuilder<EntityData>)
        entityFactories.get(EntityDataStore.entityType(identifier), entitySubtype).dataBuilder();
  }

  protected EntityData hydrate(Identifier identifier, EntityData entityData) {
    return entityFactories
        .get(EntityDataStore.entityType(identifier), entityData.getEntitySubType())
        .hydrateData(entityData);
  }

  protected void addAdditionalEvent(Identifier identifier, EntityData entityData) {
    additionalEvents.add(new AbstractMap.SimpleImmutableEntry<>(identifier, entityData));
  }

  private static Identifier parent(Identifier identifier, int distance) {
    if (distance >= identifier.path().size()) {
      return identifier;
    }
    return ImmutableIdentifier.builder()
        .from(identifier)
        .path(identifier.path().subList(distance, identifier.path().size()))
        .build();
  }

  private static Predicate<Identifier> isDuplicate(Identifier identifier) {
    return other ->
        Objects.equals(identifier.id(), other.id())
            && Objects.equals(
                EntityDataStore.entityType(identifier), EntityDataStore.entityType(other))
            && !Objects.equals(identifier.path(), other.path());
  }

  @Override
  protected CompletableFuture<Void> onListenStart() {
    blobStoreReady.get();
    valueStoreReady.get();

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
                    .sorted(EntityDataStore.COMPARATOR)
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
                          ImmutableReplayEvent.builder()
                              .type(EntityDataStore.EVENT_TYPE_ENTITIES)
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
    try (MDC.MDCCloseable closeable =
        LogContext.putCloseable(LogContext.CONTEXT.SERVICE, identifier.id())) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Entity creating: {}", identifier);
      }

      try {
        EntityData hydratedData = hydrateData(identifier, entityData);

        return entityFactories
            .get(EntityDataStore.entityType(identifier), entityData.getEntitySubType())
            .createInstance(hydratedData)
            .whenComplete(
                (entity, throwable) -> {
                  if (Objects.nonNull(entity)) {
                    if (LOGGER.isTraceEnabled()) {
                      LOGGER.trace("Entity created: {}", identifier);
                    }
                  }
                })
            .thenAccept(ignore -> CompletableFuture.completedFuture(null));
      } catch (Throwable e) {
        LogContext.error(LOGGER, e, "Failed to create entity {}", identifier);
        return CompletableFuture.completedFuture(null);
      }
    }
  }

  @Override
  protected CompletableFuture<Void> onUpdate(Identifier identifier, EntityData entityData) {
    try (MDC.MDCCloseable closeable =
        LogContext.putCloseable(LogContext.CONTEXT.SERVICE, identifier.id())) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Reloading entity: {}", identifier);
      }

      try {
        EntityData hydratedData = hydrateData(identifier, entityData);

        return entityFactories
            .get(EntityDataStore.entityType(identifier), entityData.getEntitySubType())
            .updateInstance(hydratedData)
            .thenAccept(ignore -> CompletableFuture.completedFuture(null));
      } catch (Throwable e) {
        return CompletableFuture.completedFuture(null);
      }
    }
  }

  @Override
  protected void onDelete(Identifier identifier) {
    entityFactories
        .getAll(EntityDataStore.entityType(identifier))
        .forEach(factory -> factory.deleteInstance(identifier.id()));
  }

  @Override
  protected void onFailure(Identifier identifier, Throwable throwable) {
    LogContext.error(LOGGER, throwable, "Could not save entity with id '{}'", identifier);
  }

  @Override
  public <U extends EntityData> EntityDataStore<U> forType(Class<U> type) {
    final String entityType = entityFactories.get(type).type();
    return new EntityStoreDecorator<EntityData, U>() {
      @Override
      public EntityDataStore<EntityData> getDecorated() {
        return EntityDataStoreImpl.this;
      }

      @Override
      public String[] transformPath(String... path) {
        return ObjectArrays.concat(entityType, path);
      }
    };
  }

  @Override
  public Map<String, Object> asMap(Identifier identifier, EntityData entityData)
      throws IOException {
    return valueEncodingMap.deserialize(
        identifier, valueEncoding.serialize(entityData), valueEncoding.getDefaultFormat(), false);
  }

  @Override
  public EntityData fromMap(Identifier identifier, Map<String, Object> entityData)
      throws IOException {
    return valueEncoding.deserialize(
        identifier, valueEncoding.serialize(entityData), valueEncoding.getDefaultFormat(), false);
  }

  @Override
  public EntityData fromBytes(Identifier identifier, byte[] entityData) throws IOException {
    return valueEncoding.deserialize(
        identifier, entityData, valueEncoding.getDefaultFormat(), true);
  }

  @Override
  public CompletableFuture<EntityData> put(String id, EntityData data, String... path) {
    final Identifier identifier = Identifier.from(id, path);

    try {
      Map<String, Object> map = asMap(identifier, data);

      Map<String, Object> withoutDefaults =
          defaultsStore.subtractDefaults(
              identifier, data.getEntitySubType(), map, ImmutableList.of());

      return getEventSourcing()
          .pushPartialMutationEvent(identifier, withoutDefaults)
          .whenComplete(
              (entityData, throwable) -> {
                if (Objects.nonNull(entityData)) {
                  onCreate(identifier, entityData).join();
                } else if (Objects.nonNull(throwable)) {
                  onFailure(identifier, throwable);
                }
              });
    } catch (IOException e) {
      // never reached, will fail in isUpdateValid
      return CompletableFuture.failedFuture(e);
    }
  }

  @Override
  public CompletableFuture<EntityData> patch(
      String id, Map<String, Object> partialData, String... path) {
    return patch(id, partialData, false, path);
  }

  @Override
  public CompletableFuture<EntityData> patch(
      String id, Map<String, Object> partialData, boolean skipLastModified, String... path) {
    final Identifier identifier = Identifier.from(id, path);

    Map<String, Object> patch = skipLastModified ? partialData : modifyPatch(partialData);

    byte[] payload = getValueEncoding().serialize(patch);

    // validate
    if (!isUpdateValid(identifier, payload)) {
      throw new IllegalArgumentException("Partial update for ... not valid");
    }

    try {
      EntityData merged =
          getValueEncoding()
              .deserialize(identifier, payload, getValueEncoding().getDefaultFormat(), false);

      Map<String, Object> map = asMap(identifier, merged);

      // TODO: I guess the correct way to define ignoreKeys would be in EntityFactory
      Map<String, Object> withoutDefaults =
          defaultsStore.subtractDefaults(
              identifier, merged.getEntitySubType(), map, ImmutableList.of("enabled"));

      Map<String, Object> withoutResetted =
          MapAligner.align(
              withoutDefaults,
              partialData,
              Objects::isNull,
              entityFactories.get(
                  EntityDataStore.entityType(identifier), merged.getEntitySubType()));

      return getEventSourcing()
          .pushPartialMutationEvent(identifier, withoutResetted)
          .whenComplete(
              (entityData, throwable) -> {
                if (Objects.nonNull(entityData)) {
                  onUpdate(identifier, entityData).join();
                } else if (Objects.nonNull(throwable)) {
                  onFailure(identifier, throwable);
                }
              });
    } catch (IOException e) {
      // never reached, will fail in isUpdateValid
      return CompletableFuture.failedFuture(e);
    }
  }

  private EntityData hydrateData(Identifier identifier, EntityData entityData) {
    EntityData hydratedData = entityData;

    if (LOGGER.isDebugEnabled(MARKER.DUMP)) {
      try {
        LOGGER.debug(
            MARKER.DUMP,
            "Entity data for {}:\n{}",
            identifier.asPath(),
            new String(valueEncoding.serialize(entityData), StandardCharsets.UTF_8));
      } catch (Throwable e) {
        // ignore
      }
    }

    if (entityData instanceof AutoEntity) {
      AutoEntity autoEntity = (AutoEntity) entityData;
      if (autoEntity.isAuto() && autoEntity.isAutoPersist()) {
        hydratedData = hydrate(identifier, hydratedData);

        if (!isEventStoreReadOnly) {
          try {
            Map<String, Object> map = asMap(identifier, hydratedData);

            Map<String, Object> withoutDefaults =
                defaultsStore.subtractDefaults(
                    identifier, entityData.getEntitySubType(), map, ImmutableList.of());

            putPartialWithoutTrigger(identifier, withoutDefaults).join();
            LOGGER.info(
                "Entity of type '{}' with id '{}' is in autoPersist mode, generated configuration was saved.",
                EntityDataStore.entityType(identifier),
                entityData.getId());
          } catch (IOException e) {
            LogContext.error(
                LOGGER,
                e,
                "Entity of type '{}' with id '{}' is in autoPersist mode, but generated configuration could not be saved",
                EntityDataStore.entityType(identifier),
                entityData.getId());
          }

        } else {
          LOGGER.warn(
              "Entity of type '{}' with id '{}' is in autoPersist mode, but was not persisted because the store is read only.",
              EntityDataStore.entityType(identifier),
              entityData.getId());
        }
      }
    }

    hydratedData = hydrate(identifier, hydratedData);

    if (LOGGER.isDebugEnabled(MARKER.DUMP)
        && entityData instanceof AutoEntity
        && ((AutoEntity) entityData).isAuto()) {
      try {
        LOGGER.debug(
            MARKER.DUMP,
            "Generated entity data for {}:\n{}",
            identifier.asPath(),
            new String(valueEncoding.serialize(hydratedData), StandardCharsets.UTF_8));
      } catch (Throwable e) {
        // ignore
      }
    }

    return hydratedData;
  }
}
