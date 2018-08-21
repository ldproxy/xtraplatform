/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.cfgstore.api.entity;

import java.io.IOException;

/**
 * id maybe ORG/ID, in that case a multitenant middleware would handle splitting into path and id
 *
 * @author zahnen
 */
public interface EntityStore<T extends EntityConfiguration, U extends PartialEntityConfiguration> {


    //List<String> getEntityIds();

    T getEntityData(String id);

    boolean hasEntity(String id);

    void createEntity(String id, T data) throws IOException;

    void replaceEntity(String id, T data) throws IOException;

    void updateEntity(String id, U partialData) throws IOException;

    void deleteEntity(String id) throws IOException;

    //void updateResourceOverrides(String id, T resource) throws IOException;

    //ResourceStore<T> withParent(String storeId);

    //ResourceStore<T> withChild(String storeId);

    //List<String[]> getAllPaths();
}
