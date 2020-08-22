/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app.entities.legacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ObjectArrays;
import de.ii.xtraplatform.dropwizard.domain.Jackson;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.handler.Entity;
import de.ii.xtraplatform.store.domain.entities.legacy.EntityRepository;
import de.ii.xtraplatform.store.domain.entities.legacy.EntityRepositoryChangeListener;
import de.ii.xtraplatform.store.domain.entities.legacy.RemoveEntityData;
import de.ii.xtraplatform.store.legacy.KeyValueStore;
import de.ii.xtraplatform.store.legacy.rest.AbstractGenericResourceStore;
import de.ii.xtraplatform.store.legacy.rest.ResourceSerializer;
import de.ii.xtraplatform.store.legacy.rest.ResourceStore;
import de.ii.xtraplatform.store.legacy.rest.ResourceTransaction;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.handlers.event.Publishes;
import org.apache.felix.ipojo.handlers.event.publisher.Publisher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
//TODO: why is this needed still? when deactivated entities do not work any more
@Deprecated // needed for 1.3.x service migration
@Component(publicFactory = false)
@Provides
@Instantiate
public class EntityRepositoryImpl implements EntityRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityRepositoryImpl.class);

    @Context
    BundleContext context;

    @Publishes(name = "create", topics = "create", dataKey = Entity.DATA_KEY, synchronous = true)
    private Publisher createListeners;

    @Publishes(name = "update", topics = "update", dataKey = Entity.DATA_KEY, synchronous = true)
    private Publisher updateListeners;

    @Publishes(name = "delete", topics = "delete", dataKey = Entity.DATA_KEY, synchronous = true)
    private Publisher deleteListeners;

    private final Map<String,String> entityTypes;
    private final Map<String,Object> entityDataGenerators;

    private EntityStore store;
    private KeyValueStore kvStore;
    private ObjectMapper mapper;
    private EntitySerializer entitySerializer;

    public EntityRepositoryImpl(@Requires KeyValueStore rootConfigStore, @Requires Jackson jackson) {
        LOGGER.debug("JACKSON DYNAMIC {}", jackson.getDefaultObjectMapper().getDeserializationConfig().getHandlerInstantiator());
        //this.changeListeners = new ArrayList<>();
        this.entityTypes = new LinkedHashMap<>();
        this.entityDataGenerators = new LinkedHashMap<>();
        this.entitySerializer = new EntitySerializer(jackson.getDefaultObjectMapper());
        this.store = new EntityStore(rootConfigStore, "entities", false, /*new DeepUpdater<>(jackson.getDefaultObjectMapper()),*/ entitySerializer);
        this.kvStore = rootConfigStore;
        this.mapper = jackson.getDefaultObjectMapper();
    }

    @Override
    public List<String> getEntityTypes() {
        return ImmutableList.copyOf(store.getAllPaths().stream().filter(strings -> strings.length == 2).map(strings -> strings[1]).collect(Collectors.toList()));
    }

    @Override
    public List<String> getEntityIds(String... path) {
        return ImmutableList.copyOf(store.getResourceIds(path));
    }

    @Override
    public boolean hasEntity(String id, String... path) {
        return store.getResourceIds(path).contains(id);
    }

    @Override
    public RemoveEntityData getEntityData(String id, String... path) {
        return (RemoveEntityData) store.getResource(id, path);
    }

    @Override
    public RemoveEntityData createEntity(RemoveEntityData data, String... path) throws IOException {
        validate(data);

        String[] path2 = ObjectArrays.concat("entities", path);
        LOGGER.debug("CREATE {} {}", data.getId(), path2);
        store.addResource(data, path2);

        createListeners.sendData(data);

        return data;
    }

    @Override
    public RemoveEntityData generateEntity(Map<String, Object> data, String... path) throws IOException {
        //validate(data);
        if (Objects.isNull(data) || !data.containsKey("id")) {
            throw new IOException("data map invalid");
        }
        String id = (String) data.get("id");
        Class<?> entityDataClass = getEntityDataClass(id, path);

        if (!entityDataGenerators.containsKey(entityDataClass.getName())) {
            throw new IOException("no generator found for type " + entityDataClass.getName());
        }

        String[] path2 = ObjectArrays.concat("entities", path);

        RemoveEntityData data2 = entitySerializer.deserializePartial(entityDataClass, new StringReader(mapper.writeValueAsString(data))).get();

        //RemoveEntityData data3 = (RemoveEntityData) entityDataGenerators.get(entityDataClass.getName()).generate(data2);

        LOGGER.debug("CREATE {} {}", id, path2);
        //store.addResource(data3, path2);

        //TODO
        RemoveEntityData data4 = getEntityData(id, path);
        createListeners.sendData(data4);

        return data4;
    }

    @Override
    public RemoveEntityData replaceEntity(RemoveEntityData data, String... path) throws IOException {
        validate(data);

        String[] path2 = ObjectArrays.concat("entities", path);

        store.updateResource(data, path2);

        //TODO
        EntityData data2 = getEntityData(data.getId(), path);
        updateListeners.sendData(data2);

        return data;
    }

    @Override
    public RemoveEntityData updateEntity(RemoveEntityData partialData, String... path) throws IOException {
        validate(partialData);


        store.updateResourceOverrides(partialData.getId(), partialData, path);

        //TODO
        RemoveEntityData data = getEntityData(partialData.getId(), path);
        updateListeners.sendData(data);

        return data;
    }

    @Override
    public RemoveEntityData updateEntity(String id, String partialData, String... path) throws IOException {
        //validate(partialData);


        store.updateResourceOverrides(id, partialData, path);

        //TODO
        RemoveEntityData data = getEntityData(id, path);
        updateListeners.sendData(data);

        return data;
    }

    @Override
    public void deleteEntity(String id, String... path) throws IOException {

        store.deleteResource(id, path);

        deleteListeners.sendData(id);
    }

    @Override
    public void addChangeListener(EntityRepositoryChangeListener listener) {
        //this.changeListeners.add(listener);
    }

    @Override
    public void addEntityType(String entityType, String dataType) {
        //TODO: can be more than one, maybe use DynamicSubtypeHandler
        this.entityTypes.put(entityType, dataType);
        LOGGER.debug("ENTITY TYPE {} {}", entityType, dataType);
    }

    private void validate(EntityData data) {
        Objects.requireNonNull(data, "data may not be null");
        Objects.requireNonNull(data.getId(), "data.getId() may not be null");
    }

    private String getEntityType(String id, String... path) {

        String type = entityTypes.get(path[path.length-1]);

        return type;
    }

    protected Class<?> getEntityDataClass(String id, String... path) {
        LOGGER.debug("EMPTY {} {}", id, path);

        // TODO: how to get type? only EntityInstantiator knows it
        String type = entityTypes.get(path[path.length-1]);
        //type = type.substring(0, type.lastIndexOf(".")) + ".Modifiable" + type.replace("Abstract", "").substring(type.lastIndexOf(".")+1) + "Data";
        //type = "de.ii.xtraplatform.feature.core.ModifiableServiceTestData";
        List<String> qn = Splitter.on('.').splitToList(type);
        String bundle = Joiner.on('-').join(qn.subList(2, qn.size() -1));
        LOGGER.debug("EMPTY {} {}", bundle, type);
            /*try {
                return (AbstractEntityData) Class.forName(type).getMethod("create").invoke(null);
            } catch ( IllegalAccessException | ClassNotFoundException| NoSuchMethodException | InvocationTargetException e) {
                throw new IllegalStateException("Class " + type + " not found", e);
            }*/

        //TODO: maybe get bundle by name from type
        try {
            Optional<Bundle> b2 = Arrays.stream(context.getBundles()).filter(bundle1 -> bundle1.getSymbolicName().endsWith(bundle)).findFirst();
            Bundle b = context.getBundle(bundle);
            return b2.get().loadClass(type);
        } catch (ClassNotFoundException e) {
            LOGGER.error("NOT FOUND: {}", type);
            return null;
        }
    }



    private class EntityStore extends AbstractGenericResourceStore<RemoveEntityData, ResourceStore<RemoveEntityData>> {

        public EntityStore(KeyValueStore rootConfigStore, String resourceType, ObjectMapper jsonMapper) {
            super(rootConfigStore, resourceType, jsonMapper);
        }

        public EntityStore(KeyValueStore rootConfigStore, String resourceType, ObjectMapper jsonMapper, boolean fullCache) {
            super(rootConfigStore, resourceType, jsonMapper, fullCache);
        }
// TODO: DeepUpdater and Serializer with JsonMerge, see ...
        public EntityStore(KeyValueStore rootConfigStore, String resourceType, boolean fullCache, /*DeepUpdater<AbstractEntityData> deepUpdater,*/ ResourceSerializer<RemoveEntityData> serializer) {
            super(rootConfigStore, resourceType, fullCache, /*deepUpdater,*/ serializer);
        }

        @Override
        protected RemoveEntityData createEmptyResource(String id, String... path) {
            LOGGER.debug("EMPTY {} {}", id, path);

            //String type = path[path.length-1];
            //type = type.substring(0, type.lastIndexOf(".")) + ".Modifiable" + type.substring(type.lastIndexOf(".")+1) + "Data";
            //type = "de.ii.xtraplatform.feature.core.ModifiableServiceTestData";
            //LOGGER.debug("EMPTY {} {}", type);
            /*try {
                return (AbstractEntityData) Class.forName(type).getMethod("create").invoke(null);
            } catch ( IllegalAccessException | ClassNotFoundException| NoSuchMethodException | InvocationTargetException e) {
                throw new IllegalStateException("Class " + type + " not found", e);
            }*/

            //TODO: only needed for clustering when transactions have to be rehydrated?
            return null;
        }

        @Override
        protected Class<?> getResourceClass(String id, String... path) {
            return getEntityDataClass(id ,path);
        }

        public void addResource(RemoveEntityData resource, String... path) throws IOException {
            super.writeResource(path, resource.getResourceId(), ResourceTransaction.OPERATION.ADD, resource);
        }

        public void addResource(String id, Map<String,Object> resource, String... path) throws IOException {
            super.writeResourceFromString(path, id, ResourceTransaction.OPERATION.ADD, mapper.writeValueAsString(resource));
        }

        public void updateResource(RemoveEntityData resource, String... path) throws IOException {
            super.writeResource(path, resource.getResourceId(), ResourceTransaction.OPERATION.UPDATE, resource);
        }

        //@Override
        public void updateResourceOverrides(String id, RemoveEntityData resource, String... path) throws IOException {
            String[] path2 = ObjectArrays.concat(resourceType, path);
            super.writeResource(path2, id, ResourceTransaction.OPERATION.UPDATE_OVERRIDE, resource);
        }

        public void updateResourceOverrides(String id, String resource, String... path) throws IOException {
            String[] path2 = ObjectArrays.concat(resourceType, path);
            super.writeResourceOverride(path2, id, ResourceTransaction.OPERATION.UPDATE_OVERRIDE, resource);
        }

        @Override
        public void deleteResource(String id, String... path) throws IOException {
            String[] path2 = ObjectArrays.concat(resourceType, path);
            super.writeResource(path2, id, ResourceTransaction.OPERATION.DELETE);
        }

        @Override
        public List<String> getResourceIds() {
            String[] path = {resourceType};
            return kvStore.getChildStore(path).getKeys();
        }

        public List<String> getResourceIds(String... path) {
            return kvStore.getChildStore(ObjectArrays.concat(resourceType, path)).getKeys();
        }

        @Override
        public RemoveEntityData getResource(String id) {
            String[] path = {resourceType};
            return super.getResource(path, id);
        }

        public RemoveEntityData getResource(String id, String... path) {
            return super.getResource(ObjectArrays.concat(resourceType, path), id);
        }
    }
}
