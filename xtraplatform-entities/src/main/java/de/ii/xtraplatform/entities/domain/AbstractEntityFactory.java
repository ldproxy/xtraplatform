/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import com.google.common.collect.ImmutableSet;
import de.ii.xtraplatform.base.domain.LogContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public abstract class AbstractEntityFactory<
        T extends EntityData, U extends AbstractPersistentEntity<T>>
    implements EntityFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEntityFactory.class);

  private final FactoryAssisted<T, U> assistedFactory;
  private final Map<String, U> instances;
  private final Map<String, Integer> instanceConfigurationHashes;
  private final List<Consumer<PersistentEntity>> entityListeners;
  private final List<Consumer<PersistentEntity>> entityGoneListeners;

  public AbstractEntityFactory(FactoryAssisted<T, U> assistedFactory) {
    this.assistedFactory = assistedFactory;
    this.instances = new ConcurrentHashMap<>();
    this.instanceConfigurationHashes = new ConcurrentHashMap<>();
    this.entityListeners = new ArrayList<>();
    this.entityGoneListeners = new ArrayList<>();
  }

  @Override
  public Optional<U> instance(String id) {
    return Optional.ofNullable(instances.get(id));
  }

  @Override
  public Set<U> instances() {
    return ImmutableSet.copyOf(instances.values());
  }

  @Override
  public CompletableFuture<PersistentEntity> createInstance(EntityData entityData) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("CREATING ENTITY {} {}", type(), entityData.getId());
    }

    U entity = assistedFactory.create((T) entityData);
    synchronized (this) {
      instances.put(entityData.getId(), entity);
      instanceConfigurationHashes.put(entityData.getId(), entityData.hashCode());
    }
    entity.onValidate();
    entity.onPostRegistration();
    entityListeners.forEach(listener -> listener.accept(entity));

    return CompletableFuture.completedFuture(entity);
  }

  @Override
  public CompletableFuture<PersistentEntity> updateInstance(EntityData entityData) {
    String id = entityData.getId();
    String entityTypeSingular = type().substring(0, type().length() - 1);
    U instance = instances.get(id);

    try (MDC.MDCCloseable closeable = LogContext.putCloseable(LogContext.CONTEXT.SERVICE, id)) {
      if (Objects.equals(entityData.hashCode(), instanceConfigurationHashes.get(id))) {

        LOGGER.info(
            "Not reloading configuration for {} with id '{}', no effective changes detected",
            entityTypeSingular,
            id);

        // update data anyway to enable garbage collection, will not trigger reload
        if (Objects.nonNull(instance)) {
          instance.setData((T) entityData);
        }

        return CompletableFuture.completedFuture(null);
      }

      LOGGER.info("Reloading configuration for {} with id '{}'", entityTypeSingular, id);

      CompletableFuture<PersistentEntity> reloaded = new CompletableFuture<>();

      if (Objects.nonNull(instance)) {
        // TODO this.instanceReloadListeners.put(instanceId, reloaded);

        try {
          instance.setData((T) entityData);
          synchronized (this) {
            instanceConfigurationHashes.put(id, entityData.hashCode());
          }
          reloaded.complete(instance);
        } catch (Throwable e) {
          LogContext.error(LOGGER, e, "Could not reload configuration");
          reloaded.complete(null);
        }
      } else {
        return createInstance(entityData);
      }

      return reloaded;
    }
  }

  @Override
  public void deleteInstance(String id) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("DELETING ENTITY {} {}", type(), id);
    }

    if (instances.containsKey(id)) {
      U entity = instances.get(id);
      entity.onInvalidate();
      entity.onPostUnregistration();
      synchronized (this) {
        instances.remove(id);
        instanceConfigurationHashes.remove(id);
      }
      entityGoneListeners.forEach(listener -> listener.accept(entity));
    }
  }

  @Override
  public void addEntityListener(Consumer<PersistentEntity> listener, boolean existing) {
    this.entityListeners.add(listener);
    if (existing) {
      instances.values().forEach(listener);
    }
  }

  @Override
  public void addEntityGoneListener(Consumer<PersistentEntity> listener) {
    this.entityGoneListeners.add(listener);
  }

  public interface FactoryAssisted<T extends EntityData, U extends PersistentEntity> {
    U create(T data);
  }
}
