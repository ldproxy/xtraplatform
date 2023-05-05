/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain.entities;

import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.Migration;
import de.ii.xtraplatform.store.domain.Migration.MigrationContext;
import de.ii.xtraplatform.store.domain.entities.EntityMigration.EntityMigrationContext;
import java.util.List;
import java.util.Map;

public interface EntityMigration<T extends EntityData, U extends EntityData>
    extends Migration<EntityMigrationContext<T>> {

  interface EntityMigrationContext<T extends EntityData> extends MigrationContext {
    List<T> getAll();

    boolean exists(Identifier identifier);
  }

  @Override
  default boolean isApplicable(EntityMigrationContext<T> context) {
    return context.getAll().stream().anyMatch(entityData -> isApplicable(context, entityData));
  }

  boolean isApplicable(EntityMigrationContext<T> context, T entityData);

  U migrate(T entityData);

  default Map<Identifier, ? extends EntityData> getAdditionalEntities(T entityData) {
    return Map.of();
  }
}
