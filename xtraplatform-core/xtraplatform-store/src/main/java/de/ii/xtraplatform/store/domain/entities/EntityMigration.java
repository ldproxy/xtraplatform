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
import de.ii.xtraplatform.store.domain.entities.EntityMigration.EntityMigrationContext;
import java.util.Map;

public abstract class EntityMigration<T extends EntityData, U extends EntityData>
    implements Migration<EntityMigrationContext, EntityData> {

  private final EntityMigrationContext context;

  public EntityMigration(EntityMigrationContext context) {
    this.context = context;
  }

  public interface EntityMigrationContext extends MigrationContext {
    boolean exists(Identifier identifier);
  }

  public final EntityMigrationContext getContext() {
    return context;
  }

  public abstract boolean isApplicable(EntityData entityData);

  public abstract U migrate(T entityData);

  public EntityData migrateRaw(EntityData entityData) {
    return migrate((T) entityData);
  }

  public Map<Identifier, ? extends EntityData> getAdditionalEntities(EntityData entityData) {
    return Map.of();
  }
}
