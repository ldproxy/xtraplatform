/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import de.ii.xtraplatform.entities.domain.EntityMigration.EntityMigrationContext;
import de.ii.xtraplatform.store.domain.Migration;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public abstract class EntityMigration<T extends EntityData, U extends EntityData>
    implements Migration<EntityMigrationContext, EntityData> {

  private final EntityMigrationContext context;

  public EntityMigration(EntityMigrationContext context) {
    this.context = context;
  }

  public interface EntityMigrationContext extends MigrationContext {
    boolean exists(Predicate<Identifier> matcher);
  }

  public final EntityMigrationContext getContext() {
    return context;
  }

  public boolean isApplicable(EntityData entityData) {
    return isApplicable(entityData, Optional.empty());
  }

  public abstract boolean isApplicable(EntityData entityData, Optional<EntityData> defaults);

  public U migrate(T entityData) {
    return migrate(entityData, Optional.empty());
  }

  public abstract U migrate(T entityData, Optional<T> defaults);

  public EntityData migrateRaw(EntityData entityData) {
    return migrateRaw((T) entityData, Optional.empty());
  }

  public EntityData migrateRaw(EntityData entityData, Optional<EntityData> defaults) {
    return migrate((T) entityData, defaults.map(d -> (T) d));
  }

  public Map<Identifier, ? extends EntityData> getAdditionalEntities(EntityData entityData) {
    return getAdditionalEntities(entityData, Optional.empty());
  }

  public Map<Identifier, ? extends EntityData> getAdditionalEntities(
      EntityData entityData, Optional<EntityData> defaults) {
    return Map.of();
  }
}
