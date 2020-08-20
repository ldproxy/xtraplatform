/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.kvstore.api.rest;

import de.ii.xtraplatform.api.Resource;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author zahnen
 * @param <T>
 */
public interface ResourceStore<T extends Resource> {

    List<String> getResourceIds();

    T getResource(String id);

    boolean hasResource(String id);

    void addResource(T resource) throws IOException;

    void deleteResource(String id, String... path) throws IOException;

    void updateResource(T resource) throws IOException;

    void updateResourceOverrides(String id, T resource) throws IOException;
    
    ResourceStore<T> withParent(String storeId);

    ResourceStore<T> withChild(String storeId);
    
    List<String[]> getAllPaths();
}
