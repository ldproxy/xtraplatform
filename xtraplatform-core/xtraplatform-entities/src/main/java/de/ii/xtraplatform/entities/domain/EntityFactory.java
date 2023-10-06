/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@AutoMultiBind
public interface EntityFactory {

  String type();

  default Optional<String> subType() {
    return Optional.empty();
  }

  default String fullType() {
    if (subType().isPresent()) {
      return String.format("%s/%s", type(), subType().get());
    }

    return type();
  }

  Class<? extends PersistentEntity> entityClass();

  EntityDataBuilder<? extends EntityData> dataBuilder();

  default EntityDataBuilder<? extends EntityData> superDataBuilder() {
    return dataBuilder();
  }

  EntityDataBuilder<? extends EntityData> emptyDataBuilder();

  default EntityDataBuilder<? extends EntityData> emptySuperDataBuilder() {
    return emptyDataBuilder();
  }

  Class<? extends EntityData> dataClass();

  Optional<? extends PersistentEntity> instance(String id);

  Set<? extends PersistentEntity> instances();

  CompletableFuture<PersistentEntity> createInstance(EntityData entityData);

  CompletableFuture<PersistentEntity> updateInstance(EntityData entityData);

  void deleteInstance(String id);

  default EntityData hydrateData(EntityData entityData) {
    return entityData;
  }

  default Optional<KeyPathAlias> getKeyPathAlias(String keyPath) {
    return Optional.empty();
  }

  default Optional<KeyPathAliasUnwrap> getKeyPathAliasReverse(String parentPath) {
    return Optional.empty();
  }

  default Map<String, String> getListEntryKeys() {
    return Map.of();
  }

  void addEntityListener(Consumer<PersistentEntity> listener, boolean existing);

  void addEntityGoneListener(Consumer<PersistentEntity> listener);
}
