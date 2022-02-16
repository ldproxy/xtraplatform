/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app.entities;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.biConsumerMayThrow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.store.app.EventSourcing;
import de.ii.xtraplatform.store.app.ValueDecoderBase;
import de.ii.xtraplatform.store.app.ValueDecoderEnvVarSubstitution;
import de.ii.xtraplatform.store.app.ValueDecoderWithBuilder;
import de.ii.xtraplatform.store.app.ValueEncodingJackson;
import de.ii.xtraplatform.store.domain.AbstractMergeableKeyValueStore;
import de.ii.xtraplatform.store.domain.EntityEvent;
import de.ii.xtraplatform.store.domain.EventFilter;
import de.ii.xtraplatform.store.domain.EventStore;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.ImmutableIdentifier;
import de.ii.xtraplatform.store.domain.ImmutableMutationEvent;
import de.ii.xtraplatform.store.domain.ImmutableReplayEvent;
import de.ii.xtraplatform.store.domain.KeyPathAlias;
import de.ii.xtraplatform.store.domain.MergeableKeyValueStore;
import de.ii.xtraplatform.store.domain.MutationEvent;
import de.ii.xtraplatform.store.domain.ReplayEvent;
import de.ii.xtraplatform.store.domain.ValueCache;
import de.ii.xtraplatform.store.domain.ValueEncoding;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaultsPath;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaultsStore;
import de.ii.xtraplatform.store.domain.entities.EntityFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind(interfaces = {EntityDataDefaultsStore.class})
public class EntityDataDefaultsStoreImpl extends AbstractMergeableKeyValueStore<Map<String, Object>>
    implements EntityDataDefaultsStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(EntityDataDefaultsStoreImpl.class);

  private final EntityFactory entityFactory;
  private final ValueEncodingJackson<Map<String, Object>> valueEncoding;
  private final ValueEncodingJackson<EntityDataBuilder<EntityData>> valueEncodingBuilder;
  private final ValueEncodingJackson<Map<String, Object>> valueEncodingMap;
  private final ValueEncodingJackson<EntityData> valueEncodingEntity;
  private final EventSourcing<Map<String, Object>> eventSourcing;
  private final EventStore eventStore;

  @Inject
  public EntityDataDefaultsStoreImpl(
      AppContext appContext,
      EventStore eventStore,
      Jackson jackson,
      EntityFactory entityFactory) {
    this.entityFactory = entityFactory;
    this.eventStore = eventStore;
    this.valueEncoding =
        new ValueEncodingJackson<>(
            jackson, appContext.getConfiguration().store.failOnUnknownProperties);
    this.eventSourcing =
        new EventSourcing<>(
            eventStore,
            ImmutableList.of(EntityDataDefaultsStore.EVENT_TYPE),
            valueEncoding,
            this::onStart,
            Optional.of(this::processReplayEvent),
            Optional.of(this::processMutationEvent),
            Optional.empty(),
            Optional.of(biConsumerMayThrow(this::validateDefaults)));

    valueEncoding.addDecoderPreProcessor(new ValueDecoderEnvVarSubstitution());
    valueEncoding.addDecoderMiddleware(new ValueDecoderBase<>(this::getDefaults, eventSourcing));

    this.valueEncodingBuilder =
        new ValueEncodingJackson<>(
            jackson, appContext.getConfiguration().store.failOnUnknownProperties);
    valueEncodingBuilder.addDecoderMiddleware(
        new ValueDecoderBase<>(
            this::getNewBuilder,
            new ValueCache<EntityDataBuilder<EntityData>>() {
              @Override
              public boolean isInCache(Identifier identifier) {
                return false;
              }

              @Override
              public EntityDataBuilder<EntityData> getFromCache(Identifier identifier) {
                return null;
              }
            }));

    this.valueEncodingMap =
        new ValueEncodingJackson<>(
            jackson, appContext.getConfiguration().store.failOnUnknownProperties);
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

    this.valueEncodingEntity =
        new ValueEncodingJackson<>(
            jackson, appContext.getConfiguration().store.failOnUnknownProperties);
    valueEncodingEntity.addDecoderMiddleware(
        new ValueDecoderWithBuilder<>(
            this::getBuilder,
            new ValueCache<EntityData>() {
              @Override
              public boolean isInCache(Identifier identifier) {
                return false;
              }

              @Override
              public EntityData getFromCache(Identifier identifier) {
                return null;
              }
            }));
  }

  // TODO: it seems this is needed for correct order (defaults < entities)
  //@Validate
  private void onVal() {
    // LOGGER.debug("VALID");
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

    EntityDataDefaultsPath defaultsPath = EntityDataDefaultsPath.from(event.identifier());

    List<String> subTypes =
        entityFactory.getSubTypes(defaultsPath.getEntityType(), defaultsPath.getEntitySubtype());

    // LOGGER.debug("Applying to subtypes as well: {}", subTypes);

    List<Identifier> cacheKeys = getCacheKeys(defaultsPath, subTypes);

    // LOGGER.debug("Applying to subtypes as well 2: {}", cacheKeys);

    return cacheKeys.stream()
        .map(
            cacheKey -> {
              ImmutableReplayEvent.Builder builder =
                  ImmutableReplayEvent.builder().from(event).identifier(cacheKey);
              if (!defaultsPath.getKeyPath().isEmpty()
                  && !Objects.equals(defaultsPath.getKeyPath().get(0), EVENT_TYPE)) {
                Optional<KeyPathAlias> keyPathAlias =
                    entityFactory.getKeyPathAlias(
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
        .add(
            ImmutableIdentifier.builder()
                .addPath(defaultsPath.getEntityType())
                .addAllPath(defaultsPath.getEntitySubtype())
                .id(EntityDataDefaultsStore.EVENT_TYPE)
                .build())
        .addAll(
            subTypes.stream()
                .map(
                    subType ->
                        ImmutableIdentifier.builder()
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

    Identifier defaultsIdentifier =
        subType.isPresent()
            ? ImmutableIdentifier.builder()
                .id(EntityDataDefaultsStore.EVENT_TYPE)
                .addAllPath(identifier.path())
                .addPath(subType.get().toLowerCase())
                .build()
            : ImmutableIdentifier.builder()
                .id(EntityDataDefaultsStore.EVENT_TYPE)
                .addAllPath(identifier.path())
                .build();

    EntityDataBuilder<EntityData> newBuilder =
        getBuilder(defaultsIdentifier).fillRequiredFieldsWithPlaceholders();

    try {
      byte[] payload = valueEncodingEntity.serialize(newBuilder.build());

      Map<String, Object> defaults =
          valueEncodingMap.deserialize(
              defaultsIdentifier, payload, valueEncodingBuilder.getDefaultFormat(), false);

      return new MapSubtractor().subtract(data, defaults, ignoreKeys);

    } catch (Throwable e) {
      boolean br = true;
    }

    return data;
  }

  @Override
  public Map<String, Object> asMap(Identifier identifier, EntityData entityData)
      throws IOException {
    Optional<String> subType = entityData.getEntitySubType();
    Identifier defaultsIdentifier =
        subType.isPresent()
            ? ImmutableIdentifier.builder()
                .id(EntityDataDefaultsStore.EVENT_TYPE)
                .addAllPath(identifier.path())
                .addPath(subType.get().toLowerCase())
                .build()
            : ImmutableIdentifier.builder()
                .id(EntityDataDefaultsStore.EVENT_TYPE)
                .addAllPath(identifier.path())
                .build();

    return valueEncodingMap.deserialize(
        defaultsIdentifier,
        valueEncodingEntity.serialize(entityData),
        valueEncoding.getDefaultFormat(),
        false);
  }

  @Override
  public Optional<Map<String, Object>> getAllDefaults(
      Identifier identifier, Optional<String> subType) {

    Identifier defaultsIdentifier =
        subType.isPresent()
            ? ImmutableIdentifier.builder()
                .id(EntityDataDefaultsStore.EVENT_TYPE)
                .addAllPath(identifier.path())
                .addPath(subType.get().toLowerCase())
                .build()
            : ImmutableIdentifier.builder()
                .id(EntityDataDefaultsStore.EVENT_TYPE)
                .addAllPath(identifier.path())
                .build();

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
    if (eventSourcing.isInCache(identifier)) {
      return eventSourcing.getFromCache(identifier);
    }

    return new LinkedHashMap<>();
  }

  public EntityDataBuilder<EntityData> getNewBuilder(Identifier identifier) {

    EntityDataDefaultsPath defaultsPath = EntityDataDefaultsPath.from(identifier);

    Optional<String> subtype = entityFactory.getTypeAsString(defaultsPath.getEntitySubtype());

    return entityFactory.getDataBuilder(defaultsPath.getEntityType(), subtype);
  }

  @Override
  public EntityDataBuilder<EntityData> getBuilder(Identifier identifier) {

    if (eventSourcing.isInCache(identifier)) {
      Map<String, Object> defaults = eventSourcing.getFromCache(identifier);
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
  protected CompletableFuture<Void> onStart() {

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

    return super.onStart();
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
                  EventFilter.fromPath(Path.of(EntityDataDefaultsStore.EVENT_TYPE, defaultId)));
            });

    return CompletableFuture.completedFuture(defaults);
  }
}
