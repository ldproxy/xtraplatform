/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app.entities;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import dagger.Lazy;
import de.ii.xtraplatform.store.domain.entities.EntityFactories;
import de.ii.xtraplatform.store.domain.entities.EntityFactory;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.EntityState;
import de.ii.xtraplatform.store.domain.entities.EntityState.STATE;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class EntityRegistryImpl implements EntityRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(EntityRegistryImpl.class);

  private final EntityFactories entityFactories;

  @Inject
  public EntityRegistryImpl(Lazy<Set<EntityFactory>> entityFactories) {
    this.entityFactories = new EntityFactories(entityFactories);
  }

  @Override
  public <T extends PersistentEntity> List<T> getEntitiesForType(Class<T> type) {
    return entityFactories.getAll(type).stream()
        .flatMap(entityFactory -> entityFactory.instances().stream())
        .filter(persistentEntity -> ((EntityState)persistentEntity).getState() == STATE.ACTIVE)
        .map(type::cast)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public <T extends PersistentEntity> Optional<T> getEntity(Class<T> type, String id) {
    return entityFactories.getAll(type).stream()
        .map(entityFactory -> entityFactory.instance(id))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(persistentEntity -> ((EntityState)persistentEntity).getState() == STATE.ACTIVE)
        .map(type::cast)
        .findFirst();
  }

  @Override
  public Optional<PersistentEntity> getEntity(String type, String id) {
    return entityFactories.getAll(type).stream()
        .map(entityFactory -> entityFactory.instance(id))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(persistentEntity -> (PersistentEntity) persistentEntity)
        .findFirst();
  }

  @Override
  public Optional<EntityState.STATE> getEntityState(String type, String id) {
    // return Optional.ofNullable(entityStates.get(type + id)).map(EntityState::getState);
    return getEntity(type, id).map(persistentEntity -> ((EntityState) persistentEntity).getState());
  }

  @Override
  public <T extends PersistentEntity> void addEntityListener(
      Class<T> type, Consumer<T> listener, boolean existing) {
    entityFactories
        .getAll(type)
        .forEach(
            entityFactory ->
                entityFactory.addEntityListener(
                    entity -> listener.accept(type.cast(entity)), existing));
  }

  @Override
  public <T extends PersistentEntity> void addEntityGoneListener(
      Class<T> type, Consumer<T> listener) {
    entityFactories
        .getAll(type)
        .forEach(
            entityFactory ->
                entityFactory.addEntityGoneListener(entity -> listener.accept(type.cast(entity))));
  }
}
