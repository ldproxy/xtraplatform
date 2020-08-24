/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain.entities;

import de.ii.xtraplatform.store.domain.Identifier;
import java.util.Map;

public interface EntityMigration<T extends EntityData, U extends EntityData> {

  long getSourceVersion();

  long getTargetVersion();

  EntityDataBuilder<T> getDataBuilder();

  U migrate(T entityData);

  Map<Identifier, EntityData> getAdditionalEntities(Identifier identifier, T entityData);
}
