/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entity.api;

import java.util.List;
import java.util.Optional;

/**
 * @author zahnen
 */
public interface EntityRegistry {
    <T extends PersistentEntity> List<T> getEntitiesForType(Class<T> clazz, String type);
    <T extends PersistentEntity> Optional<T> getEntity(Class<T> clazz, String type, String id);
}
