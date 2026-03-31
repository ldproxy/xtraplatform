/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import dagger.Lazy;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class EntityFactoriesImpl implements EntityFactories {

  private final Lazy<Set<EntityFactory>> entityFactories;

  @Inject
  public EntityFactoriesImpl(Lazy<Set<EntityFactory>> entityFactories) {
    this.entityFactories = entityFactories;
  }

  @Override
  public EntityFactory get(String entityType) {
    return entityFactories.get().stream()
        .filter(entityFactory -> entityFactory.type().equalsIgnoreCase(entityType))
        .findFirst()
        .orElseThrow(
            () ->
                new NoSuchElementException(
                    String.format("No factory found for entity type %s", entityType)));
  }

  @Override
  public EntityFactory get(String entityType, String subType) {
    return entityFactories.get().stream()
        .filter(
            entityFactory ->
                entityFactory.type().equalsIgnoreCase(entityType)
                    && entityFactory
                        .subType()
                        .filter(
                            entityFactorySubType -> entityFactorySubType.equalsIgnoreCase(subType))
                        .isPresent())
        .findFirst()
        .orElseThrow(
            () ->
                new NoSuchElementException(
                    String.format("No factory found for entity type %s/%s", entityType, subType)));
  }

  @Override
  public EntityFactory get(String entityType, Optional<String> subType) {
    return subType.isPresent() ? get(entityType, subType.get()) : get(entityType);
  }

  @Override
  public EntityFactory get(Class<? extends EntityData> dataClass) {
    return entityFactories.get().stream()
        .filter(entityFactory -> Objects.equals(dataClass, entityFactory.dataClass()))
        .findFirst()
        .orElseThrow(
            () ->
                new NoSuchElementException("No factory found for entity data class " + dataClass));
  }

  @Override
  public Set<EntityFactory> getAll(Class<? extends PersistentEntity> entityClass) {
    return entityFactories.get().stream()
        .filter(entityFactory -> entityClass.isAssignableFrom(entityFactory.entityClass()))
        .collect(Collectors.toSet());
  }

  @Override
  public Set<EntityFactory> getAll(String entityType) {
    return entityFactories.get().stream()
        .filter(entityFactory -> Objects.equals(entityType, entityFactory.type()))
        .collect(Collectors.toSet());
  }

  @Override
  public List<String> getSubTypes(String entityType, List<String> entitySubType) {
    String specificEntityType = getSpecificEntityType(entityType, getTypeAsString(entitySubType));

    return entityFactories.get().stream()
        .filter(
            entityFactory ->
                entityFactory.fullType().startsWith(specificEntityType)
                    && !Objects.equals(entityFactory.fullType(), specificEntityType))
        .map(entityFactory -> entityFactory.subType().map(String::toLowerCase))
        .filter(Optional::isPresent)
        .map(Optional::get)
        // .map(subType -> Splitter.on('/').splitToList(subType))
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public Set<String> getTypes() {
    return entityFactories.get().stream()
        .map(EntityFactory::type)
        .collect(ImmutableSet.toImmutableSet());
  }

  private String getSpecificEntityType(String entityType, Optional<String> entitySubType) {
    return entitySubType.isPresent()
        ? String.format("%s/%s", entityType, entitySubType.get().toLowerCase(Locale.ROOT))
        : entityType;
  }

  @Override
  public Optional<String> getTypeAsString(List<String> entitySubtype) {
    if (entitySubtype.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(Joiner.on('/').join(entitySubtype));
  }
}
