/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.app;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.biConsumerMayThrow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import dagger.Lazy;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.StoreConfiguration;
import de.ii.xtraplatform.base.domain.Substitutions;
import de.ii.xtraplatform.entities.domain.AbstractMergeableKeyValueStore;
import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.EntityDataBuilder;
import de.ii.xtraplatform.entities.domain.EntityDataDefaultsPath;
import de.ii.xtraplatform.entities.domain.EntityDataDefaultsStore;
import de.ii.xtraplatform.entities.domain.EntityDataStore;
import de.ii.xtraplatform.entities.domain.EntityEvent;
import de.ii.xtraplatform.entities.domain.EntityFactoriesImpl;
import de.ii.xtraplatform.entities.domain.EntityFactory;
import de.ii.xtraplatform.entities.domain.EventFilter;
import de.ii.xtraplatform.entities.domain.EventStore;
import de.ii.xtraplatform.entities.domain.ImmutableMutationEvent;
import de.ii.xtraplatform.entities.domain.ImmutableReplayEvent;
import de.ii.xtraplatform.entities.domain.KeyPathAlias;
import de.ii.xtraplatform.entities.domain.MergeableKeyValueStore;
import de.ii.xtraplatform.entities.domain.MutationEvent;
import de.ii.xtraplatform.entities.domain.ReplayEvent;
import de.ii.xtraplatform.values.api.ValueDecoderBase;
import de.ii.xtraplatform.values.api.ValueDecoderEnvVarSubstitution;
import de.ii.xtraplatform.values.api.ValueDecoderWithBuilder;
import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.ImmutableIdentifier;
import de.ii.xtraplatform.values.domain.ValueCache;
import de.ii.xtraplatform.values.domain.ValueEncoding;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind(interfaces = {EntityDataDefaultsStore.class, AppLifeCycle.class})
public class EntityDataDefaultsStoreImpl extends AbstractMergeableKeyValueStore<Map<String, Object>>
    implements EntityDataDefaultsStore, AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(EntityDataDefaultsStoreImpl.class);

  private final EntityFactoriesImpl entityFactories;
  private final ValueEncodingJacksonWithNesting<Map<String, Object>> valueEncoding;
  private final ValueEncodingJacksonWithNesting<EntityDataBuilder<EntityData>> valueEncodingBuilder;
  private final ValueEncodingJacksonWithNesting<Map<String, Object>> valueEncodingMap;
  private final ValueEncodingJacksonWithNesting<EntityData> valueEncodingEntity;
  private final EventSourcing<Map<String, Object>> eventSourcing;
  private final EventStore eventStore;
  private final CompletableFuture<Void> ready;

  @Inject
  public EntityDataDefaultsStoreImpl(
      AppContext appContext,
      EventStore eventStore,
      Jackson jackson,
      Substitutions substitutions,
      Lazy<Set<EntityFactory>> entityFactories) {
    StoreConfiguration store = appContext.getConfiguration().getStore();
    this.entityFactories = new EntityFactoriesImpl(entityFactories);
    this.eventStore = eventStore;
    this.ready = new CompletableFuture<>();
    this.valueEncoding =
        new ValueEncodingJacksonWithNesting<>(
            jackson, store.getMaxYamlFileSize(), store.isFailOnUnknownProperties());
    this.eventSourcing =
        new EventSourcing<>(
            eventStore,
            ImmutableList.of(EntityDataDefaultsStore.EVENT_TYPE),
            valueEncoding,
            this::onListenStart,
            Optional.of(this::processReplayEvent),
            Optional.of(this::processMutationEvent),
            Optional.empty(),
            Optional.empty(),
            Optional.of(biConsumerMayThrow(this::validateDefaults)));

    valueEncoding.addDecoderPreProcessor(new ValueDecoderEnvVarSubstitution(substitutions));
    valueEncoding.addDecoderMiddleware(new ValueDecoderBase<>(this::getDefaults, eventSourcing));
    // eventSourcing.start();

    this.valueEncodingBuilder =
        new ValueEncodingJacksonWithNesting<>(
            jackson, store.getMaxYamlFileSize(), store.isFailOnUnknownProperties());
    valueEncodingBuilder.addDecoderMiddleware(
        new ValueDecoderBase<>(
            this::getNewBuilder,
            new ValueCache<EntityDataBuilder<EntityData>>() {
              @Override
              public boolean has(Identifier identifier) {
                return false;
              }

              @Override
              public boolean has(Predicate<Identifier> keyMatcher) {
                return false;
              }

              @Override
              public EntityDataBuilder<EntityData> get(Identifier identifier) {
                return null;
              }
            }));

    this.valueEncodingMap =
        new ValueEncodingJacksonWithNesting<>(
            jackson, store.getMaxYamlFileSize(), store.isFailOnUnknownProperties());
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

    this.valueEncodingEntity =
        new ValueEncodingJacksonWithNesting<>(
            jackson, store.getMaxYamlFileSize(), store.isFailOnUnknownProperties());
    valueEncodingEntity.addDecoderMiddleware(
        new ValueDecoderWithBuilder<>(
            this::getBuilder,
            new ValueCache<EntityData>() {
              @Override
              public boolean has(Identifier identifier) {
                return false;
              }

              @Override
              public boolean has(Predicate<Identifier> keyMatcher) {
                return false;
              }

              @Override
              public EntityData get(Identifier identifier) {
                return null;
              }
            }));
  }

  @Override
  public int getPriority() {
    return 200;
  }

  @Override
  public CompletionStage<Void> onStart(boolean isStartupAsync) {
    eventSourcing.start();

    return CompletableFuture.completedFuture(null);
  }

  private List<ReplayEvent> processReplayEvent(ReplayEvent event) {
    return processEvent(event)
        .map(entityEvent -> (ReplayEvent) entityEvent)
        .collect(Collectors.toList());
  }

  private List<MutationEvent> processMutationEvent(MutationEvent event) {
    return processEvent(event)
        .map(entityEvent -> ImmutableMutationEvent.builder().from(entityEvent).build())
        .collect(Collectors.toList());
  }

  // TODO: onEmit middleware
  private Stream<EntityEvent> processEvent(EntityEvent event) {

    if (valueEncoding.isEmpty(event.payload()) || !valueEncoding.isSupported(event.format())) {
      return Stream.empty();
    }

    EntityDataDefaultsPath defaultsPath =
        EntityDataDefaultsPath.from(event.identifier(), entityFactories.getTypes());

    List<String> subTypes =
        entityFactories.getSubTypes(defaultsPath.getEntityType(), defaultsPath.getEntitySubtype());

    // LOGGER.debug("Applying to subtypes as well: {}", subTypes);

    List<Identifier> cacheKeys = getCacheKeys(defaultsPath, subTypes);

    // LOGGER.debug("Applying to subtypes as well 2: {} ### {}", event.identifier(), cacheKeys);

    return cacheKeys.stream()
        .map(
            cacheKey -> {
              ImmutableReplayEvent.Builder builder =
                  ImmutableReplayEvent.builder().from(event).identifier(cacheKey);
              if (!defaultsPath.getKeyPath().isEmpty()
                  && !Objects.equals(defaultsPath.getKeyPath().get(0), EVENT_TYPE)) {
                int entityIndex = cacheKey.path().indexOf(defaultsPath.getEntityType());
                Optional<KeyPathAlias> keyPathAlias =
                    entityFactories
                        .get(cacheKey.path().get(entityIndex), cacheKey.path().get(entityIndex + 1))
                        .getKeyPathAlias(
                            defaultsPath.getKeyPath().get(defaultsPath.getKeyPath().size() - 1));
                try {
                  byte[] nestedPayload =
                      valueEncoding.nestPayload(
                          event.payload(), event.format(), defaultsPath.getKeyPath(), keyPathAlias);
                  builder.payload(nestedPayload);
                } catch (IOException e) {
                  LogContext.error(LOGGER, e, "Deserialization error");
                }
              }

              return builder.build();
            });
  }

  private List<Identifier> getCacheKeys(
      EntityDataDefaultsPath defaultsPath, List<String> subTypes) {

    return ImmutableList.<Identifier>builder()
        .add(defaultsPath.asIdentifier())
        .addAll(
            subTypes.stream()
                .map(
                    subType ->
                        ImmutableIdentifier.builder()
                            .addAllPath(Lists.reverse(defaultsPath.getGroups()))
                            .addPath(defaultsPath.getEntityType())
                            .addPath(subType)
                            .id(EntityDataDefaultsStore.EVENT_TYPE)
                            .build())
                .collect(Collectors.toList()))
        .build();
  }

  @Override
  public Map<String, Object> subtractDefaults(
      Identifier identifier,
      Optional<String> subType,
      Map<String, Object> data,
      List<String> ignoreKeys) {

    Identifier defaultsIdentifier = EntityDataStore.defaults(identifier, subType);

    EntityDataBuilder<EntityData> newBuilder =
        getBuilder(defaultsIdentifier).fillRequiredFieldsWithPlaceholders();

    try {
      byte[] payload = valueEncodingEntity.serialize(newBuilder.build());

      Map<String, Object> defaults =
          valueEncodingMap.deserialize(
              defaultsIdentifier, payload, valueEncodingBuilder.getDefaultFormat(), false);

      return MapSubtractor.subtract(
          data, defaults, ignoreKeys, getFactory(defaultsIdentifier).getListEntryKeys());

    } catch (Throwable e) {
      boolean br = true;
    }

    return data;
  }

  public EntityFactory getFactory(Identifier identifier) {
    EntityDataDefaultsPath defaultsPath =
        EntityDataDefaultsPath.from(identifier, entityFactories.getTypes());

    Optional<String> subtype = entityFactories.getTypeAsString(defaultsPath.getEntitySubtype());

    return entityFactories.get(defaultsPath.getEntityType(), subtype);
  }

  @Override
  public Map<String, Object> asMap(Identifier identifier, EntityData entityData)
      throws IOException {
    Optional<String> subType = entityData.getEntitySubType();
    Identifier defaultsIdentifier = EntityDataStore.defaults(identifier, subType);

    return valueEncodingMap.deserialize(
        defaultsIdentifier,
        valueEncodingEntity.serialize(entityData),
        valueEncoding.getDefaultFormat(),
        false);
  }

  @Override
  public Optional<Map<String, Object>> getAllDefaults(
      Identifier identifier, Optional<String> subType) {

    Identifier defaultsIdentifier = EntityDataStore.defaults(identifier, subType);

    Optional<EntityDataBuilder<EntityData>> newBuilder =
        Optional.ofNullable(getBuilder(defaultsIdentifier).fillRequiredFieldsWithPlaceholders());

    if (newBuilder.isPresent()) {
      try {
        EntityData data = newBuilder.get().build();

        byte[] payload = valueEncodingEntity.serialize(data);

        Map<String, Object> defaults =
            valueEncodingMap.deserialize(
                defaultsIdentifier, payload, valueEncodingBuilder.getDefaultFormat(), false);

        // TODO
        defaults =
            defaults.entrySet().stream()
                .filter(entry -> !Objects.equals(entry.getValue(), "__DEFAULT__"))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        return Optional.ofNullable(defaults);

      } catch (Throwable e) {
        boolean br = true;
      }
    }

    return Optional.empty();
  }

  private Map<String, Object> getDefaults(Identifier identifier) {
    if (eventSourcing.has(identifier)) {
      return eventSourcing.get(identifier);
    }

    for (int i = 1; i < identifier.path().size(); i++) {
      ImmutableIdentifier parent =
          ImmutableIdentifier.builder()
              .from(identifier)
              .path(identifier.path().subList(i, identifier.path().size()))
              .build();
      if (eventSourcing.has(parent)) {
        try {
          Map<String, Object> deserialize =
              valueEncodingMap.deserialize(
                  parent,
                  valueEncodingEntity.serialize(eventSourcing.get(parent)),
                  valueEncoding.getDefaultFormat(),
                  false);

          return deserialize;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }

    return new LinkedHashMap<>();
  }

  public EntityDataBuilder<EntityData> getNewBuilder(Identifier identifier) {
    EntityDataDefaultsPath defaultsPath =
        EntityDataDefaultsPath.from(identifier, entityFactories.getTypes());

    Optional<String> subtype = entityFactories.getTypeAsString(defaultsPath.getEntitySubtype());

    return (EntityDataBuilder<EntityData>)
        entityFactories.get(defaultsPath.getEntityType(), subtype).dataBuilder();
  }

  @Override
  public EntityDataBuilder<EntityData> getBuilder(Identifier identifier) {

    if (eventSourcing.has(identifier)) {
      Map<String, Object> defaults = eventSourcing.get(identifier);
      byte[] payload = valueEncodingBuilder.serialize(defaults);

      try {
        return valueEncodingBuilder.deserialize(
            identifier, payload, valueEncodingBuilder.getDefaultFormat(), false);
      } catch (IOException e) {
        LogContext.error(LOGGER, e, "Cannot load defaults for '{}'", identifier.asPath());
      }
    }

    return getNewBuilder(identifier);
  }

  private void validateDefaults(Identifier identifier, Map<String, Object> defaults)
      throws IOException {
    byte[] payload = valueEncodingBuilder.serialize(defaults);
    valueEncodingBuilder.deserialize(
        identifier, payload, valueEncodingBuilder.getDefaultFormat(), false);
  }

  @Override
  protected ValueEncoding<Map<String, Object>> getValueEncoding() {
    return valueEncoding;
  }

  @Override
  protected EventSourcing<Map<String, Object>> getEventSourcing() {
    return eventSourcing;
  }

  @Override
  public <U extends Map<String, Object>> MergeableKeyValueStore<U> forType(Class<U> type) {
    return null;
  }

  // TODO: load defaults from EntityFactory that weren't loaded by event
  @Override
  protected CompletableFuture<Void> onListenStart() {

    identifiers()
        .forEach(
            identifier -> {
              // EntityDataBuilder<EntityData> builder = get(identifier);

              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Loaded defaults: {}", identifier);
              }

              /*try {
                  builder.build();
              } catch (Throwable e) {
                  LOGGER.debug("Error: {}", e.getMessage());
              }*/

            });

    ready.complete(null);

    return super.onListenStart();
  }

  @Override
  public CompletableFuture<Map<String, Object>> patch(
      String id, Map<String, Object> partialData, String... path) {
    String defaultId = Joiner.on('.').skipNulls().join(path);
    Map<String, Object> defaults = partialData;

    if (has(id, path)) {
      Identifier defaultsIdentifier = Identifier.from(id, path);

      Optional<EntityDataBuilder<EntityData>> newBuilder =
          Optional.ofNullable(getBuilder(defaultsIdentifier).fillRequiredFieldsWithPlaceholders());

      if (newBuilder.isPresent()) {
        try {

          ObjectMapper mapper =
              valueEncodingEntity.getMapper(valueEncodingEntity.getDefaultFormat());

          byte[] serialize = valueEncodingEntity.serialize(partialData);

          mapper.readerForUpdating(newBuilder.get()).readValue(serialize);

          byte[] payload = valueEncodingEntity.serialize(newBuilder.get().build());

          defaults =
              valueEncodingMap.deserialize(
                  defaultsIdentifier, payload, valueEncodingBuilder.getDefaultFormat(), false);

        } catch (Throwable e) {
          LogContext.error(LOGGER, e, "Deserialization error");
        }
      }
    }

    // TODO
    defaults =
        defaults.entrySet().stream()
            .filter(entry -> !Objects.equals(entry.getValue(), "__DEFAULT__"))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    put(defaultId, defaults)
        .thenRun(
            () -> {
              eventStore.replay(
                  EventFilter.fromPath(Path.of(EntityDataDefaultsStore.EVENT_TYPE, defaultId)),
                  false);
            });

    return CompletableFuture.completedFuture(defaults);
  }

  @Override
  public CompletableFuture<Void> onReady() {
    return ready;
  }
}
