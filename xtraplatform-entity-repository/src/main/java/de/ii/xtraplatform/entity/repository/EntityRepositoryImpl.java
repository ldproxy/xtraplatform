package de.ii.xtraplatform.entity.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ObjectArrays;
import de.ii.xsf.configstore.api.KeyValueStore;
import de.ii.xsf.configstore.api.rest.AbstractGenericResourceStore;
import de.ii.xsf.configstore.api.rest.ResourceSerializer;
import de.ii.xsf.configstore.api.rest.ResourceStore;
import de.ii.xsf.configstore.api.rest.ResourceTransaction;
import de.ii.xsf.core.util.json.DeepUpdater;
import de.ii.xsf.dropwizard.api.Jackson;
import de.ii.xtraplatform.entity.api.AbstractEntityData;
import de.ii.xtraplatform.entity.api.EntityRepository;
import de.ii.xtraplatform.entity.api.EntityRepositoryChangeListener;
import de.ii.xtraplatform.service.api.ModifiableServiceData;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.handlers.event.Publishes;
import org.apache.felix.ipojo.handlers.event.publisher.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author zahnen
 */
@Component(publicFactory = false)
@Provides
@Instantiate
public class EntityRepositoryImpl implements EntityRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityRepositoryImpl.class);

    @Publishes(name = "create", topics = "create", dataKey = "data", synchronous = true)
    private Publisher createListeners;

    @Publishes(name = "update", topics = "update", dataKey = "data", synchronous = true)
    private Publisher updateListeners;

    @Publishes(name = "delete", topics = "delete", dataKey = "data", synchronous = true)
    private Publisher deleteListeners;

    private final Map<String, AbstractEntityData> entityBuffer;

    //private final List<EntityRepositoryChangeListener> changeListeners;

    private EntityStore store;
    private KeyValueStore kvStore;

    public EntityRepositoryImpl(@Requires KeyValueStore rootConfigStore, @Requires Jackson jackson) {
        //this.changeListeners = new ArrayList<>();
        this.entityBuffer = new LinkedHashMap<>();
        this.store = new EntityStore(rootConfigStore, "entities", jackson.getDefaultObjectMapper());
        this.kvStore = rootConfigStore;
    }

    @Override
    public List<String> getEntityIds() {
        return ImmutableList.copyOf(store.getResourceIds());
    }

    @Override
    public boolean hasEntity(String id) {
        return store.getResourceIds().contains(id);
    }

    @Override
    public AbstractEntityData getEntityData(String id) {
        return store.getResource(id);
    }

    @Override
    public AbstractEntityData createEntity(AbstractEntityData data, String... path) throws IOException {
        validate(data);

        String[] path2 = ObjectArrays.concat("entities", path);
        LOGGER.debug("CREATE {} {}", data.getId(), path2);
        store.addResource(data, path2);

        createListeners.sendData(data);

        return data;
    }

    @Override
    public AbstractEntityData replaceEntity(AbstractEntityData data) throws IOException {
        validate(data);

        store.updateResource(data);

        updateListeners.sendData(data);

        return data;
    }

    @Override
    public AbstractEntityData updateEntity(AbstractEntityData partialData) throws IOException {
        validate(partialData);

        //TODO
        AbstractEntityData data = null;

        store.updateResourceOverrides(data.getId(), data);

        updateListeners.sendData(data);

        return data;
    }

    @Override
    public void deleteEntity(String id) throws IOException {

        store.deleteResource(id);

        deleteListeners.sendData(id);
    }

    @Override
    public void addChangeListener(EntityRepositoryChangeListener listener) {
        //this.changeListeners.add(listener);
    }

    private void validate(AbstractEntityData data) {
        Objects.requireNonNull(data, "data may not be null");
        Objects.requireNonNull(data.getId(), "data.getId() may not be null");
    }



    private class EntityStore extends AbstractGenericResourceStore<AbstractEntityData, ResourceStore<AbstractEntityData>> {

        public EntityStore(KeyValueStore rootConfigStore, String resourceType, ObjectMapper jsonMapper) {
            super(rootConfigStore, resourceType, jsonMapper);
        }

        public EntityStore(KeyValueStore rootConfigStore, String resourceType, ObjectMapper jsonMapper, boolean fullCache) {
            super(rootConfigStore, resourceType, jsonMapper, fullCache);
        }
// TODO: DeepUpdater and Serializer with JsonMerge, see ...
        public EntityStore(KeyValueStore rootConfigStore, String resourceType, boolean fullCache, DeepUpdater<AbstractEntityData> deepUpdater, ResourceSerializer<AbstractEntityData> serializer) {
            super(rootConfigStore, resourceType, fullCache, deepUpdater, serializer);
        }

        @Override
        protected AbstractEntityData createEmptyResource(String id, String... path) {
            LOGGER.debug("EMPTY {} {}", id, path);

            String type = path[path.length-1];
            //TODO type = type.substring(0, type.lastIndexOf(".")) + ".Modifiable" + type.substring(type.lastIndexOf(".")+1);
            type = "de.ii.xtraplatform.service.api.ModifiableServiceData";
            LOGGER.debug("EMPTY {} {}", type);
            try {
                return (AbstractEntityData) Class.forName(type).getMethod("create").invoke(null);
            } catch ( IllegalAccessException | ClassNotFoundException| NoSuchMethodException | InvocationTargetException e) {
                throw new IllegalStateException("Class " + type + " not found", e);
            }
        }

        public void addResource(AbstractEntityData resource, String... path) throws IOException {
            super.writeResource(path, resource.getResourceId(), ResourceTransaction.OPERATION.ADD, resource);
        }

        @Override
        public void updateResourceOverrides(String id, AbstractEntityData resource) throws IOException {
            String[] path = {resourceType};
            super.writeResource(path, id, ResourceTransaction.OPERATION.UPDATE_OVERRIDE, resource);
        }

        @Override
        public void deleteResource(String id) throws IOException {
            String[] path = {resourceType};
            super.writeResource(path, id, ResourceTransaction.OPERATION.DELETE);
        }

        @Override
        public List<String> getResourceIds() {
            String[] path = {resourceType};
            return kvStore.getChildStore(path).getKeys();
        }

        @Override
        public AbstractEntityData getResource(String id) {
            String[] path = {resourceType};
            return super.getResource(path, id);
        }
    }
}
