/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain.entities;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** @author zahnen */
public interface EntityRegistry {
  <T extends PersistentEntity> List<T> getEntitiesForType(Class<T> type);

  <T extends PersistentEntity> Optional<T> getEntity(Class<T> type, String id);

  Optional<PersistentEntity> getEntity(String type, String id);

  Optional<EntityState.STATE> getEntityState(String type, String id);

  <T extends PersistentEntity> void addEntityListener(
      Class<T> type, Consumer<T> listener, boolean existing);

  <T extends PersistentEntity> void addEntityGoneListener(Class<T> type, Consumer<T> listener);
}
