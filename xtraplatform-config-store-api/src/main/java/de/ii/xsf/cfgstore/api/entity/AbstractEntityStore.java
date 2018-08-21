/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.cfgstore.api.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xsf.configstore.api.KeyValueStore;
import de.ii.xsf.configstore.api.rest.AbstractGenericResourceStore;
import de.ii.xsf.configstore.api.rest.ResourceSerializer;
import de.ii.xsf.configstore.api.rest.ResourceStore;
import de.ii.xsf.configstore.api.rest.ResourceTransaction;
import de.ii.xsf.core.util.json.DeepUpdater;

import java.io.IOException;
import java.util.List;

/**
 * @author zahnen
 */
public abstract class AbstractEntityStore<T extends EntityConfiguration, U extends PartialEntityConfiguration> implements EntityStore<T, U> {

    RStore store;
    KeyValueStore kvStore;

    AbstractEntityStore(KeyValueStore rootConfigStore, String resourceType, ObjectMapper jsonMapper) {
        this.store = new RStore(rootConfigStore, resourceType, jsonMapper);
        this.kvStore = rootConfigStore;
    }

    @Override
    public T getEntityData(String id) {
        return store.getResource(id);
    }

    @Override
    public boolean hasEntity(String id) {
        return false;
    }

    @Override
    public void createEntity(String id, T data) throws IOException {
        store.addResource(data);
    }

    @Override
    public void replaceEntity(String id, T data) throws IOException {
        store.updateResourceOverrides(id, data);
    }

    @Override
    public void updateEntity(String id, U partialData) throws IOException {

    }

    public List<String> getIds() {
        return store.getResourceIds();
    }

    @Override
    public void deleteEntity(String id) throws IOException {
        store.deleteResource(id);
    }

    private class RStore extends AbstractGenericResourceStore<T, ResourceStore<T>> {

        public RStore(KeyValueStore rootConfigStore, String resourceType, ObjectMapper jsonMapper) {
            super(rootConfigStore, resourceType, jsonMapper);
        }

        public RStore(KeyValueStore rootConfigStore, String resourceType, ObjectMapper jsonMapper, boolean fullCache) {
            super(rootConfigStore, resourceType, jsonMapper, fullCache);
        }

        public RStore(KeyValueStore rootConfigStore, String resourceType, boolean fullCache, DeepUpdater<T> deepUpdater, ResourceSerializer<T> serializer) {
            super(rootConfigStore, resourceType, fullCache, deepUpdater, serializer);
        }

        @Override
        protected T createEmptyResource(String id, String... path) {
            return (T) new EntityConfigurationTestImpl();
        }

        @Override
        public void addResource(T resource) throws IOException {
            String[] path = {resourceType};
            super.writeResource(path, resource.getResourceId(), ResourceTransaction.OPERATION.ADD, resource);
        }

        @Override
        public void updateResourceOverrides(String id, T resource) throws IOException {
            String[] path = {resourceType};
            super.writeResource(path, id, ResourceTransaction.OPERATION.UPDATE_OVERRIDE, resource);
        }

        @Override
        public void deleteResource(String id) throws IOException {
            String[] path = {resourceType};
            super.writeResource(path, id, ResourceTransaction.OPERATION.DELETE);
        }

        @Override
        protected Class<?> getResourceClass(String id, String... path) {
            return null;
        }

        @Override
        public List<String> getResourceIds() {
            String[] path = {resourceType};
            return kvStore.getChildStore(path).getKeys();
        }

        @Override
        public T getResource(String id) {
            String[] path = {resourceType};
            return super.getResource(path, id);
        }
    }
}
