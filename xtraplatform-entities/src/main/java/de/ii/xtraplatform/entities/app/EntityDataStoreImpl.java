/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.Maps;
import com.google.common.collect.ObjectArrays;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dagger.Lazy;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.base.domain.StoreConfiguration;
import de.ii.xtraplatform.base.domain.Substitutions;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
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
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
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
  private final boolean asyncStartup;
  private final ExecutorService executorService;

  @Inject
  public EntityDataStoreImpl(
      AppContext appContext,
      EventStore eventStore,
      Jackson jackson,
      Substitutions substitutions,
      Lazy<Set<EntityFactory>> entityFactories,
      EntityDataDefaultsStore defaultsStore,
      ResourceStore blobStore,
      ValueStore valueStore) {
    this(
        appContext,
        eventStore,
        jackson,
        substitutions,
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
      Substitutions substitutions,
      Lazy<Set<EntityFactory>> entityFactories,
      EntityDataDefaultsStore defaultsStore,
      ResourceStore blobStore,
      ValueStore valueStore,
      boolean noDefaults) {
    super();
    StoreConfiguration store = appContext.getConfiguration().getStore();
    this.isEventStoreReadOnly = eventStore.isReadOnly();
    this.entityFactories = new EntityFactoriesImpl(entityFactories);
    this.additionalEvents = new ConcurrentLinkedQueue<>();
    this.valueEncoding =
        new ValueEncodingJacksonWithNesting<>(
            jackson, store.getMaxYamlFileSize(), store.isFailOnUnknownProperties());
    this.valueEncodingMap =
        new ValueEncodingJacksonWithNesting<>(
            jackson, store.getMaxYamlFileSize(), store.isFailOnUnknownProperties());
    this.eventSourcing =
        new EventSourcing<>(
            eventStore,
            EntityDataStore.EVENT_TYPES,
            valueEncoding,
            this::onListenStart,
            Optional.of(this::processEvent),
            Optional.empty(),
            Optional.of((i, e) -> onUpdate(i, e, false)),
            Optional.of((i, e) -> onUpdate(i, e, true)));
    this.defaultsStore = defaultsStore;
    this.blobStoreReady = blobStore.onReady()::join;
    this.valueStoreReady = valueStore.onReady()::join;
    this.noDefaults = noDefaults;
    this.asyncStartup = appContext.getConfiguration().getModules().isStartupAsync();
    this.executorService =
        MoreExecutors.getExitingExecutorService(
            (ThreadPoolExecutor)
                Executors.newCachedThreadPool(
                    new ThreadFactoryBuilder().setNameFormat("entity.lifecycle-%d").build()));

    valueEncoding.addDecoderPreProcessor(new ValueDecoderEnvVarSubstitution(substitutions));
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
                return new LinkedHashMap<>();
              }
            }));
  }

  @Override
  public int getPriority() {
    return 210;
  }

  @Override
  public CompletionStage<Void> onStart(boolean isStartupAsync) {
    defaultsStore.onReady().join();
    eventSourcing.start();

    return CompletableFuture.completedFuture(null);
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
  @SuppressWarnings({
    "PMD.CyclomaticComplexity",
    "PMD.NPathComplexity",
    "PMD.CognitiveComplexity",
    "PMD.CollapsibleIfStatements"
  })
  private List<ReplayEvent> processEvent(ReplayEvent event) {

    if (valueEncoding.isEmpty(event.payload()) || !valueEncoding.isSupported(event.format())) {
      return List.of();
    }

    if (!event.isDelete()
        && EntityDataStore.EVENT_TYPE_ENTITIES.equals(event.type())
        && eventSourcing.has(isDuplicate(event.identifier()))) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "Ignoring entity '{}' from {} because it already exists. An entity can only exist in a single group.",
            event.asPathNoType(),
            event.source().orElse("UNKNOWN"));
      }
      return List.of();
    }

    if (!event.isDelete()
        && EntityDataStore.EVENT_TYPE_ENTITIES.equals(event.type())
        && eventSourcing.has(event.identifier())) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "Ignoring entity '{}' from {} because it already exists. An entity can only exist in a single source, use overrides to update it from another source.",
            event.asPathNoType(),
            event.source().orElse("UNKNOWN"));
      }
    }

    if (!EntityDataStore.EVENT_TYPE_OVERRIDES.equals(event.type())) {
      return List.of(event);
    }

    EntityDataOverridesPath overridesPath =
        EntityDataOverridesPath.from(event.identifier(), entityFactories.getTypes());

    Identifier cacheKey = overridesPath.asIdentifier();

    // override without matching entity
    if (!eventSourcing.has(cacheKey)) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("Ignoring override '{}', no matching entity found", event.asPath());
        return List.of();
      }
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

    return List.of(builder.build());
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
            ignore -> {
              if (asyncStartup) {
                List<List<Identifier>> groups =
                    identifiers().stream()
                        .sorted(EntityDataStore.COMPARATOR)
                        .reduce(
                            new ArrayList<>(),
                            (list, identifier) -> {
                              if (list.isEmpty()
                                  || EntityDataStore.COMPARATOR.compare(
                                          list.get(list.size() - 1)
                                              .get(list.get(list.size() - 1).size() - 1),
                                          identifier)
                                      != 0) {
                                ArrayList<Identifier> newList = new ArrayList<>();
                                newList.add(identifier);
                                list.add(newList);
                              } else {
                                list.get(list.size() - 1).add(identifier);
                              }
                              return list;
                            },
                            (l1, l2) -> l1);

                return groups.stream()
                    .reduce(
                        CompletableFuture.completedFuture(null),
                        (completableFuture, identifiers) ->
                            completableFuture.thenCompose(
                                ignore2 ->
                                    CompletableFuture.allOf(
                                        identifiers.stream()
                                            .map(
                                                identifier ->
                                                    CompletableFuture.runAsync(
                                                        () ->
                                                            onCreate(identifier, get(identifier))
                                                                .join(),
                                                        executorService))
                                            .toArray(CompletableFuture[]::new))),
                        (first, second) -> first.thenCompose(ignore2 -> second));
              }

              return identifiers().stream()
                  .sorted(EntityDataStore.COMPARATOR)
                  .reduce(
                      CompletableFuture.completedFuture((Void) null),
                      (completableFuture, identifier) ->
                          completableFuture.thenCompose(
                              ignore2 -> onCreate(identifier, get(identifier))),
                      (first, second) -> first.thenCompose(ignore2 -> second));
            })
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
                  if (Objects.nonNull(entity) && LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Entity created: {}", identifier);
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
  protected CompletableFuture<Void> onUpdate(
      Identifier identifier, EntityData entityData, boolean force) {
    try (MDC.MDCCloseable closeable =
        LogContext.putCloseable(LogContext.CONTEXT.SERVICE, identifier.id())) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Reloading entity: {}", identifier);
      }

      try {
        EntityData hydratedData = hydrateData(identifier, entityData);

        return entityFactories
            .get(EntityDataStore.entityType(identifier), entityData.getEntitySubType())
            .updateInstance(hydratedData, force)
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
  public EntityDataStore<EntityData> forType(String type) {
    return new EntityStoreDecorator<EntityData, EntityData>() {
      @Override
      public EntityDataStore<EntityData> getDecorated() {
        return EntityDataStoreImpl.this;
      }

      @Override
      public String[] transformPath(String... path) {
        return ObjectArrays.concat(type, path);
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
          defaultsStore.subtractDefaults(identifier, data.getEntitySubType(), map);

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
  public String hash(EntityData value) {
    return valueEncoding.hash(value);
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
          defaultsStore.subtractDefaults(identifier, merged.getEntitySubType(), map);

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
                  onUpdate(identifier, entityData, false).join();
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

    if (!entityData.getEnabled()) {
      return entityData;
    }

    EntityData hydratedData = hydrate(identifier, entityData);

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
