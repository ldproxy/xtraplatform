/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app.entities;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.ObjectArrays;
import dagger.Lazy;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.store.app.EventSourcing;
import de.ii.xtraplatform.store.app.ValueDecoderBase;
import de.ii.xtraplatform.store.app.ValueDecoderEnvVarSubstitution;
import de.ii.xtraplatform.store.app.ValueDecoderWithBuilder;
import de.ii.xtraplatform.store.app.ValueEncodingJackson;
import de.ii.xtraplatform.store.domain.AbstractMergeableKeyValueStore;
import de.ii.xtraplatform.store.domain.EventStore;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.ImmutableIdentifier;
import de.ii.xtraplatform.store.domain.ImmutableReplayEvent;
import de.ii.xtraplatform.store.domain.KeyPathAlias;
import de.ii.xtraplatform.store.domain.KeyPathAliasUnwrap;
import de.ii.xtraplatform.store.domain.ReplayEvent;
import de.ii.xtraplatform.store.domain.ValueCache;
import de.ii.xtraplatform.store.domain.ValueEncoding;
import de.ii.xtraplatform.store.domain.entities.AutoEntity;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaultsStore;
import de.ii.xtraplatform.store.domain.entities.EntityDataOverridesPath;
import de.ii.xtraplatform.store.domain.entities.EntityDataStore;
import de.ii.xtraplatform.store.domain.entities.EntityFactoriesImpl;
import de.ii.xtraplatform.store.domain.entities.EntityFactory;
import de.ii.xtraplatform.store.domain.entities.EntityStoreDecorator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
  private final ValueEncodingJackson<EntityData> valueEncoding;
  private final ValueEncodingJackson<Map<String, Object>> valueEncodingMap;
  private final EventSourcing<EntityData> eventSourcing;
  private final EntityDataDefaultsStore defaultsStore;

  @Inject
  public EntityDataStoreImpl(
      AppContext appContext,
      EventStore eventStore,
      Jackson jackson,
      Lazy<Set<EntityFactory>> entityFactories,
      EntityDataDefaultsStore defaultsStore) {
    this.isEventStoreReadOnly = eventStore.isReadOnly();
    this.entityFactories = new EntityFactoriesImpl(entityFactories);
    this.additionalEvents = new ConcurrentLinkedQueue<>();
    this.valueEncoding =
        new ValueEncodingJackson<>(
            jackson, appContext.getConfiguration().getStore().isFailOnUnknownProperties());
    this.valueEncodingMap =
        new ValueEncodingJackson<>(
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

    valueEncoding.addDecoderPreProcessor(new ValueDecoderEnvVarSubstitution());
    valueEncoding.addDecoderMiddleware(
        new ValueDecoderWithBuilder<>(this::getBuilder, eventSourcing));
    valueEncoding.addDecoderMiddleware(
        new ValueDecoderEntitySubtype(this::getBuilder, eventSourcing));
    /*TODO valueEncoding.addDecoderMiddleware(
    new ValueDecoderEntityDataMigration(
        eventSourcing, entityFactories, this::addAdditionalEvent));*/
    valueEncoding.addDecoderMiddleware(new ValueDecoderIdValidator());

    valueEncodingMap.addDecoderMiddleware(
        new ValueDecoderBase<>(
            identifier -> new LinkedHashMap<>(),
            new ValueCache<Map<String, Object>>() {
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
        && eventSourcing.isInCache(isDuplicate(event.identifier()))) {
      LOGGER.warn(
          "Ignoring entity '{}' from {} because it already exists. An entity can only exist in a single group.",
          event.asPathNoType(),
          event.source().orElse("UNKNOWN"));
      return ImmutableList.of();
    }

    if (!event.isDelete()
        && event.type().equals(EntityDataStore.EVENT_TYPE_ENTITIES)
        && eventSourcing.isInCache(event.identifier())) {
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
    if (!eventSourcing.isInCache(cacheKey)) {
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

  protected EntityDataBuilder<EntityData> getBuilder(Identifier identifier) {
    return (EntityDataBuilder<EntityData>)
        entityFactories.get(entityType(identifier)).superDataBuilder();
  }

  protected EntityDataBuilder<EntityData> getBuilder(Identifier identifier, String entitySubtype) {
    Identifier defaultsIdentifier = defaults(identifier, entitySubtype);

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
        entityFactories.get(entityType(identifier), entitySubtype).dataBuilder();
  }

  protected EntityData hydrate(Identifier identifier, EntityData entityData) {
    return entityFactories
        .get(entityType(identifier), entityData.getEntitySubType())
        .hydrateData(entityData);
  }

  protected void addAdditionalEvent(Identifier identifier, EntityData entityData) {
    additionalEvents.add(new AbstractMap.SimpleImmutableEntry<>(identifier, entityData));
  }

  public static String entityType(Identifier identifier) {
    if (identifier.path().isEmpty()) {
      throw new IllegalArgumentException("Invalid path, no entity type found.");
    }
    return identifier.path().get(identifier.path().size() - 1);
  }

  private static List<String> entityGroup(Identifier identifier) {
    return identifier.path().size() > 1
        ? Lists.reverse(identifier.path().subList(0, identifier.path().size() - 1))
        : List.of();
  }

  private static Identifier defaults(Identifier identifier, String subType) {
    return ImmutableIdentifier.builder()
        .id(EntityDataDefaultsStore.EVENT_TYPE)
        .path(entityGroup(identifier))
        .addPath(entityType(identifier))
        .addPath(subType.toLowerCase())
        .build();
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
            && Objects.equals(entityType(identifier), entityType(other))
            && !Objects.equals(identifier.path(), other.path());
  }

  @Override
  protected CompletableFuture<Void> onListenStart() {
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
                    // TODO: set priority per entity type (for now alphabetic works:
                    //  codelists < providers < services)
                    .sorted(Comparator.comparing(EntityDataStoreImpl::entityType))
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
            .get(entityType(identifier), entityData.getEntitySubType())
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
            .get(entityType(identifier), entityData.getEntitySubType())
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
        .getAll(entityType(identifier))
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
          subtractResetted(
              withoutDefaults,
              partialData,
              entityFactories.get(entityType(identifier), merged.getEntitySubType()));

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

  private Map<String, Object> subtractResetted(
      Map<String, Object> source, Map<String, Object> potentialNulls, EntityFactory entityFactory) {
    Map<String, Object> result = new LinkedHashMap<>();

    source.forEach(
        (key, value) -> {
          if (potentialNulls.containsKey(key) && Objects.isNull(potentialNulls.get(key))) {
            return;
          }

          Object newValue =
              value instanceof Map && potentialNulls.get(key) instanceof Map
                  ? subtractResetted(
                      (Map<String, Object>) value,
                      (Map<String, Object>) potentialNulls.get(key),
                      entityFactory)
                  : value instanceof List && potentialNulls.get(key) instanceof List
                      ? subtractResetted(
                          (List<Object>) value,
                          (List<Object>) potentialNulls.get(key),
                          entityFactory,
                          key)
                      : value instanceof List && potentialNulls.get(key) instanceof Map
                          ? subtractResetted(
                              (List<Object>) value, (Map<String, Object>) potentialNulls.get(key))
                          : value;

          result.put(key, newValue);
        });

    return result;
  }

  private List<Object> subtractResetted(
      List<Object> source,
      List<Object> potentialNulls,
      EntityFactory entityFactory,
      String parentKey) {
    if (!reverseAliases.containsKey(parentKey)) {
      return source;
    }

    List<Object> result = new ArrayList<>();
    KeyPathAliasUnwrap aliasUnwrap = reverseAliases.get(parentKey);

    Map<String, Object> resetted =
        subtractResetted(
            aliasUnwrap.wrapMap(source), aliasUnwrap.wrapMap(potentialNulls), entityFactory);
    resetted.forEach(
        (s, o) -> {
          ((Map<String, Object>) o).remove("buildingBlock");
        });

    List<Object> collect =
        resetted.entrySet().stream()
            .flatMap(
                entry -> {
                  return entityFactory
                      .getKeyPathAlias(entry.getKey())
                      .map(
                          keyPathAlias1 -> {
                            return keyPathAlias1
                                .wrapMap((Map<String, Object>) entry.getValue())
                                .values()
                                .stream()
                                .flatMap(
                                    coll -> {
                                      return ((List<Object>) coll).stream();
                                    });
                          })
                      .orElse(Stream.empty());
                })
            .collect(Collectors.toList());

    return collect;
  }

  private List<Object> subtractResetted(List<Object> source, Map<String, Object> potentialNulls) {
    List<Object> result = new ArrayList<>();

    source.forEach(
        item -> {
          if (potentialNulls.containsKey(item) && Objects.isNull(potentialNulls.get(item))) {
            return;
          }

          result.add(item);
        });

    return result;
  }

  // TODO: get from entityFactory
  private static Map<String, KeyPathAliasUnwrap> reverseAliases =
      ImmutableMap.of(
          "api",
          value ->
              ((List<Map<String, Object>>) value)
                  .stream()
                      .map(
                          buildingBlock ->
                              new AbstractMap.SimpleImmutableEntry<String, Object>(
                                  ((String) buildingBlock.get("buildingBlock")).toLowerCase(),
                                  buildingBlock))
                      .collect(
                          ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));

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
                entityType(identifier),
                entityData.getId());
          } catch (IOException e) {
            LogContext.error(
                LOGGER,
                e,
                "Entity of type '{}' with id '{}' is in autoPersist mode, but generated configuration could not be saved",
                entityType(identifier),
                entityData.getId());
          }

        } else {
          LOGGER.warn(
              "Entity of type '{}' with id '{}' is in autoPersist mode, but was not persisted because the store is read only.",
              entityType(identifier),
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
