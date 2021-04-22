/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app.entities;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.runtime.domain.LogContext;
import de.ii.xtraplatform.runtime.domain.LogContext.MARKER;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.EntityState;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.apache.felix.ipojo.whiteboard.Whiteboards;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author zahnen */
@Component(publicFactory = false)
@Provides
@Instantiate
@Whiteboards(
    whiteboards = {
      @Wbp(
          filter = "(objectClass=de.ii.xtraplatform.store.domain.entities.PersistentEntity)",
          onArrival = "onEntityArrival",
          onDeparture = "onEntityDeparture"),
      @Wbp(
          filter = "(objectClass=de.ii.xtraplatform.store.domain.entities.EntityState)",
          onArrival = "onEntityStateArrival",
          onDeparture = "onEntityStateDeparture")
    })
public class EntityRegistryImpl implements EntityRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(EntityRegistryImpl.class);

  private final BundleContext context;
  private final Set<PersistentEntity> entities;
  private final Map<String, EntityState> entityStates;
  private final List<BiConsumer<String, PersistentEntity>> entityListeners;
  private final List<Consumer<PersistentEntity>> entityGoneListeners;
  private final List<Consumer<EntityState>> entityStateListeners;

  public EntityRegistryImpl(@Context BundleContext context) {
    this.context = context;
    this.entities = new HashSet<>();
    this.entityStates = new ConcurrentHashMap<>();
    this.entityListeners = new ArrayList<>();
    this.entityGoneListeners = new ArrayList<>();
    this.entityStateListeners = new ArrayList<>();
  }

  private synchronized void onEntityArrival(ServiceReference<PersistentEntity> ref) {
    try {
      final PersistentEntity entity = context.getService(ref);

      if (Objects.nonNull(entity)) {
        entities.add(entity);

        if (LOGGER.isDebugEnabled(MARKER.DI)) {
          LOGGER.debug(MARKER.DI, "Registered entity: {} {}", entity.getClass(), entity.getId());
        }

        String instanceId = (String) ref.getProperty("instance.name");
        entityListeners.forEach(listener -> listener.accept(instanceId, entity));
      }
    } catch (Throwable e) {
      LogContext.error(LOGGER, e, "Unexpected error");
    }
  }

  private synchronized void onEntityDeparture(ServiceReference<PersistentEntity> ref) {
    final PersistentEntity entity = context.getService(ref);

    if (Objects.nonNull(entity)) {
      entityGoneListeners.forEach(listener -> listener.accept(entity));

      entities.remove(entity);

      if (LOGGER.isDebugEnabled(MARKER.DI)) {
        LOGGER.debug(MARKER.DI, "Deregistered entity: {} {}", entity.getClass(), entity.getId());
      }
    }
  }

  private synchronized void onEntityStateArrival(ServiceReference<EntityState> ref) {
    try {
      final EntityState entity = context.getService(ref);

      if (Objects.nonNull(entity)) {

        if (LOGGER.isDebugEnabled(MARKER.DI)) {
          LOGGER.debug(
              MARKER.DI, "Registered entity STATE: {} {}", entity.getClass(), entity.getId());
        }

        this.entityStates.put(entity.getEntityType() + entity.getId(), entity);
        entity.addListener(
            entityState -> entityStateListeners.forEach(listener -> listener.accept(entityState)));
      }
    } catch (Throwable e) {
      LogContext.error(LOGGER, e, "Unexpected error");
    }
  }

  private synchronized void onEntityStateDeparture(ServiceReference<EntityState> ref) {
    final EntityState entity = context.getService(ref);

    if (Objects.nonNull(entity)) {

      if (LOGGER.isDebugEnabled(MARKER.DI)) {
        LOGGER.debug(
            MARKER.DI, "Deregistered entity STATE: {} {}", entity.getClass(), entity.getId());
      }

      this.entityStates.remove(entity.getEntityType() + entity.getId());
    }
  }

  @Override
  public <T extends PersistentEntity> List<T> getEntitiesForType(Class<T> type) {
    return entities.stream()
        .filter(persistentEntity -> type.isAssignableFrom(persistentEntity.getClass()))
        .map(type::cast)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public <T extends PersistentEntity> Optional<T> getEntity(Class<T> type, String id) {
    return entities.stream()
        .filter(
            persistentEntity ->
                type.isAssignableFrom(persistentEntity.getClass())
                    && persistentEntity.getId().equals(id))
        .map(type::cast)
        .findFirst();
  }

  @Override
  public Optional<PersistentEntity> getEntity(String type, String id) {
    return entities.stream()
        .filter(
            persistentEntity ->
                Objects.equals(type, persistentEntity.getType())
                    && Objects.equals(id, persistentEntity.getId()))
        .findFirst();
  }

  @Override
  public Optional<EntityState.STATE> getEntityState(String type, String id) {
    return Optional.ofNullable(entityStates.get(type + id)).map(EntityState::getState);
  }

  @Override
  public void addEntityStateListener(Consumer<EntityState> listener) {
    this.entityStateListeners.add(listener);
  }

  @Override
  public void addEntityListener(BiConsumer<String, PersistentEntity> listener) {
    this.entityListeners.add(listener);
    // entities.forEach(entity -> listener.accept(entity.getId(), entity));
  }

  @Override
  public <T extends PersistentEntity> void addEntityListener(
      Class<T> type, Consumer<T> listener, boolean existing) {
    this.entityListeners.add(
        (id, entity) -> {
          if (type.isAssignableFrom(entity.getClass())) {
            listener.accept(type.cast(entity));
          }
        });
    if (existing) {
      getEntitiesForType(type).forEach(listener);
    }
  }

  @Override
  public <T extends PersistentEntity> void addEntityGoneListener(
      Class<T> type, Consumer<T> listener) {
    this.entityGoneListeners.add(
        (entity) -> {
          if (type.isAssignableFrom(entity.getClass())) {
            listener.accept(type.cast(entity));
          }
        });
  }
}
