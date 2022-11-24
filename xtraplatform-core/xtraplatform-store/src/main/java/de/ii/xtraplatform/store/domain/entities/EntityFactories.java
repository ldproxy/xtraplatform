/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain.entities;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EntityFactories {

  EntityFactory get(String entityType);

  EntityFactory get(String entityType, String subType);

  EntityFactory get(String entityType, Optional<String> subType);

  EntityFactory get(Class<? extends EntityData> dataClass);

  Set<EntityFactory> getAll(Class<? extends PersistentEntity> entityClass);

  Set<EntityFactory> getAll(String entityType);

  List<String> getSubTypes(String entityType, List<String> entitySubType);

  Optional<String> getTypeAsString(List<String> entitySubtype);
}
