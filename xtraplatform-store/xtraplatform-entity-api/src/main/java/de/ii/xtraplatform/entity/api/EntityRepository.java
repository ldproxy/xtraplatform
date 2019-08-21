/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entity.api;

import org.apache.http.impl.entity.EntitySerializer;

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

    RemoveEntityData getEntityData(String id, String... path);

    RemoveEntityData createEntity(RemoveEntityData data, String... path) throws IOException;

    RemoveEntityData generateEntity(Map<String, Object> data, String... path) throws IOException;

    RemoveEntityData replaceEntity(RemoveEntityData data, String... path) throws IOException;

    RemoveEntityData updateEntity(RemoveEntityData partialData, String... path) throws IOException;

    RemoveEntityData updateEntity(String id, String partialData, String... path) throws IOException;

    void deleteEntity(String id, String... path) throws IOException;

    void addChangeListener(EntityRepositoryChangeListener listener);

    void addEntityType(String entityType, String dataType);
}