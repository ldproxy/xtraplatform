/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entity.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * id maybe TYPE/ORG/ID, in that case a multitenant middleware would handle splitting into path and id
 *
 * @author zahnen
 */
public interface EntityRepository {

    String ID_SEPARATOR = "/";


    List<String> getEntityTypes();

    List<String> getEntityIds(String... path);

    boolean hasEntity(String id, String... path);

    AbstractEntityData getEntityData(String id, String... path);

    AbstractEntityData createEntity(AbstractEntityData data, String... path) throws IOException;

    AbstractEntityData generateEntity(Map<String, Object> data, String... path) throws IOException;

    AbstractEntityData replaceEntity(AbstractEntityData data) throws IOException;

    AbstractEntityData updateEntity(AbstractEntityData partialData, String... path) throws IOException;

    AbstractEntityData updateEntity(String id, String partialData, String... path) throws IOException;

    void deleteEntity(String id) throws IOException;

    void addChangeListener(EntityRepositoryChangeListener listener);

    void addEntityType(String entityType, String dataType);
}