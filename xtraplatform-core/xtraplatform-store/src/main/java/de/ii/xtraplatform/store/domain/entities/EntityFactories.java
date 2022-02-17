/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain.entities;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import dagger.Lazy;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityFactory;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EntityFactories {

  private final Lazy<Set<EntityFactory>> entityFactories;

  public EntityFactories(Lazy<Set<EntityFactory>> entityFactories) {
    this.entityFactories = entityFactories;
  }

  public EntityFactory get(String entityType) {
    return entityFactories.get().stream()
        .filter(entityFactory -> Objects.equals(entityType, entityFactory.type()))
        .findFirst()
        .orElseThrow(
            () ->
                new NoSuchElementException(
                    String.format("No factory found for entity type %s", entityType)));
  }

  public EntityFactory get(String entityType, String subType) {
    return entityFactories.get().stream()
        .filter(
            entityFactory ->
                Objects.equals(entityType, entityFactory.type())
                    && entityFactory.subType().filter(s -> Objects.equals(subType, s)).isPresent())
        .findFirst()
        .orElseThrow(
            () ->
                new NoSuchElementException(
                    String.format("No factory found for entity type %s/%s", entityType, subType)));
  }

  public EntityFactory get(String entityType, Optional<String> subType) {
    return subType.isPresent() ? get(entityType, subType.get()) : get(entityType);
  }

  public EntityFactory get(Class<? extends EntityData> dataClass) {
    return entityFactories.get().stream()
        .filter(entityFactory -> Objects.equals(dataClass, entityFactory.dataClass()))
        .findFirst()
        .orElseThrow(
            () ->
                new NoSuchElementException("No factory found for entity data class " + dataClass));
  }

  public Set<EntityFactory> getAll(Class<? extends PersistentEntity> entityClass) {
    return entityFactories.get().stream()
        .filter(entityFactory -> entityClass.isAssignableFrom(entityFactory.entityClass()))
        .collect(Collectors.toSet());
  }

  public Set<EntityFactory> getAll(String entityType) {
    return entityFactories.get().stream()
        .filter(entityFactory -> Objects.equals(entityType, entityFactory.type()))
        .collect(Collectors.toSet());
  }

  public List<String> getSubTypes(String entityType, List<String> entitySubType) {
    String specificEntityType = getSpecificEntityType(entityType, getTypeAsString(entitySubType));

    return entityFactories.get().stream()
        .filter(
            entityFactory ->
                entityFactory.fullType().startsWith(specificEntityType)
                    && !Objects.equals(entityFactory.fullType(), specificEntityType))
        .map(entityFactory -> entityFactory.subType())
        .filter(Optional::isPresent)
        .map(Optional::get)
        // .map(subType -> Splitter.on('/').splitToList(subType))
        .collect(ImmutableList.toImmutableList());
  }

  private String getSpecificEntityType(String entityType, Optional<String> entitySubType) {
    return entitySubType.isPresent()
        ? String.format("%s/%s", entityType, entitySubType.get().toLowerCase())
        : entityType;
  }

  public Optional<String> getTypeAsString(List<String> entitySubtype) {
    if (entitySubtype.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(Joiner.on('/').join(entitySubtype));
  }
}
