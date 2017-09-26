/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.xsf.configstore.api.rest;

import de.ii.xsf.core.api.Resource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import de.ii.xsf.configstore.api.KeyValueStore;
import de.ii.xsf.configstore.api.KeyNotFoundException;
import de.ii.xsf.configstore.api.Transaction;
import de.ii.xsf.configstore.api.rest.ResourceTransaction.OPERATION;
import de.ii.xsf.core.api.exceptions.ResourceNotFound;
import de.ii.xsf.core.api.exceptions.WriteError;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xsf.core.util.json.DeepUpdater;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 *
 * @author zahnen
 * @param <T>
 * @param <U>
 */
public abstract class AbstractGenericResourceStore<T extends Resource, U extends ResourceStore> implements ResourceStore<T> {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(AbstractGenericResourceStore.class);

    protected static final String OVERRIDES_STORE_NAME = "#overrides#";

    private final KeyValueStore rootConfigStore;
    protected final String resourceType;
    private final Map<String, ResourceCache<T>> resourceCache;
    private final Lock resourceCacheLock;
    private final Map<String, ReadWriteLock> resourceLocks;
    private final Lock resourceLocksLock;
    private final boolean fullCache;
    private final String[] defaultPath;
    private final Map<String, ProxyAdapter> proxies;
    private final DeepUpdater<T> deepUpdater;
    private final ResourceSerializer<T> serializer;

    public AbstractGenericResourceStore(KeyValueStore rootConfigStore, String resourceType, ObjectMapper jsonMapper) {
        this(rootConfigStore, resourceType, false, new DeepUpdater<T>(jsonMapper), new GenericResourceSerializer<T>(jsonMapper));
    }

    public AbstractGenericResourceStore(KeyValueStore rootConfigStore, String resourceType, ObjectMapper jsonMapper, boolean fullCache) {
        this(rootConfigStore, resourceType, fullCache, new DeepUpdater<T>(jsonMapper), new GenericResourceSerializer<T>(jsonMapper));
    }

    public AbstractGenericResourceStore(KeyValueStore rootConfigStore, String resourceType, boolean fullCache, DeepUpdater<T> deepUpdater, ResourceSerializer<T> serializer) {
        this.rootConfigStore = rootConfigStore;
        this.resourceType = resourceType;

        //
        this.resourceCache = new HashMap<>();
        this.resourceCacheLock = new ReentrantLock();
        this.resourceLocks = new HashMap<>();
        this.resourceLocksLock = new ReentrantLock();
        this.fullCache = fullCache;
        String[] defaultPath = {resourceType};
        this.defaultPath = defaultPath;
        this.proxies = new HashMap<>();
        this.deepUpdater = deepUpdater;
        this.serializer = serializer;
    }

    abstract protected T createEmptyResource();

    protected String getPathString(String[] path) {
        return Joiner.on('/').skipNulls().join(path);
    }

    protected String getPathString(String[] path, String id) {
        return Joiner.on('/').skipNulls().join(getPathString(path), id);
    }

    protected String[] getPathArray(String path) {
        return Iterables.toArray(Splitter.on('/').omitEmptyStrings().trimResults().split(path), String.class);
    }

    protected ReadWriteLock getResourceLock(String[] path, String id) {
        String pid = getPathString(path, id);
        resourceLocksLock.lock();
        try {
            if (!resourceLocks.containsKey(pid)) {
                this.resourceLocks.put(pid, new ReentrantReadWriteLock());
            }
        } finally {
            resourceLocksLock.unlock();
        }

        return resourceLocks.get(pid);
    }

    protected KeyValueStore getResourceStore(String[] path) {
        if (path.length == 0) {
            // TODO: throw
            return null;
        }

        KeyValueStore resourceStore = rootConfigStore.getChildStore(path);

        resourceCacheLock.lock();
        try {
            if (!resourceCache.containsKey(getPathString(path))) {

                //TODO: new Map ...
                ResourceCache<T> cache = new ResourceCache<>(fullCache);
                cache.add(new CopyOnWriteArrayList<>(resourceStore.getKeys()));
                this.resourceCache.put(getPathString(path), cache);
            }

        } finally {
            resourceCacheLock.unlock();
        }

        return resourceStore;
    }

    protected ResourceCache<T> getResourceCache(String[] path) {
        // lazy
        if (!resourceCache.containsKey(getPathString(path))) {
            getResourceStore(path);
        }

        return resourceCache.get(getPathString(path));
    }

    protected void fillCache() {
        for (String[] path: getAllPaths()) {
            List<String> resourceIds = getResourceIds(path);
            if (fullCache) {
                for (String id : resourceIds) {
                    getResource(path, id);
                }
            }
        }
    }

    protected List<String> getResourceIds(String[] path) {
        return getResourceCache(path).getResourceIds();
    }

    protected boolean hasResource(String[] path, String id) {
        return getResourceCache(path).hasResource(id);
    }

    protected T getResource(String[] path, String id) {
        T resource = this.createEmptyResource();

        getResourceLock(path, id).readLock().lock();
        try {
            if (fullCache && getResourceCache(path).hasResource(id)) {
                return getResourceCache(path).get(id);
            }

            resource = readResource(path, id, resource);
            LOGGER.getLogger().debug("deserialized resource with id {}", id);
            getResourceCache(path).put(id, resource);

        } catch (KeyNotFoundException ex) {
            // Resource does not exist
            return null;
        } catch (Exception ex) {
            // TODO ...
            LOGGER.getLogger().error("Error deserializing resource with id {}", id, ex);
        } finally {
            getResourceLock(path, id).readLock().unlock();
        }

        return resource;
    }

    protected T readResource(String[] path, String id, T resource) throws IOException, KeyNotFoundException {
        Reader resourceReader = getResourceStore(path).getValueReader(id);
        LOGGER.getLogger().debug("deserializing resource with id {}", id);
        serializer.deserialize(resource, resourceReader);

        try {
            Reader customReader = getResourceStore(path).getChildStore(OVERRIDES_STORE_NAME).getValueReader(id);
            deepUpdater.applyUpdate(resource, customReader);
        } catch (KeyNotFoundException ex) {
            // no override when not found
        }

        return resource;
    }

    protected void writeResource(String[] path, String resourceId, OPERATION operation) throws IOException {
        writeResource(path, resourceId, operation, null);
    }

    protected void writeResource(String[] path, String resourceId, OPERATION operation, T resource) throws IOException {

        ResourceTransaction<T> resourceTransaction = openTransaction(path, resourceId, operation);
        if (resource != null) {
            if (operation == OPERATION.UPDATE_OVERRIDE) {
                // create clone of resource to be updated (needed for cache rollbacks)
                // TODO
                T merged = getResource(path, resourceId);//deepUpdater.applyUpdate(createEmptyResource(), getResource(path, resourceId));
                // merge changes to cloned object
                merged = deepUpdater.applyUpdate(merged, serializer.serializeUpdate(resource));

                resourceTransaction.write(merged);
            } else {
            resourceTransaction.write(resource);
        }
        }
        executeTransaction(resourceTransaction);
    }
    
    protected ResourceTransaction<T> openTransaction(String[] path, String resourceId, OPERATION operation) {
        return openTransaction(new ResourceTransaction<T>(path, resourceId, operation));
    }
    
    // create all the sub-transactions that do the actual io
    protected ResourceTransaction<T> openTransaction(ResourceTransaction<T> resourceTransaction) {
        LOGGER.getLogger().debug("OPEN RT {} {} {}", resourceTransaction.getPath(), resourceTransaction.getResourceId(), getResourceLock(resourceTransaction.getPath(), resourceTransaction.getResourceId()));

        resourceTransaction.setSerializer(serializer);
        resourceTransaction.setResourceLock(getResourceLock(resourceTransaction.getPath(), resourceTransaction.getResourceId()).writeLock());
        resourceTransaction.setCache(getResourceCache(resourceTransaction.getPath()));

        // TODO: DELETE_ALL
        if (resourceTransaction.getOperation() != OPERATION.DELETE) {
            resourceTransaction.setEmptyResource(createEmptyResource());
        }
        
        List<Transaction> storeTransactions = new ArrayList<>();
        switch (resourceTransaction.getOperation()) {
            case DELETE:
                storeTransactions.add(getResourceStore(resourceTransaction.getPath()).getChildStore(OVERRIDES_STORE_NAME).openDeleteTransaction(resourceTransaction.getResourceId()));
            case DELETE_ALL:
                storeTransactions.add(getResourceStore(resourceTransaction.getPath()).openDeleteTransaction(resourceTransaction.getResourceId()));
                break;
            case UPDATE_OVERRIDE:
                storeTransactions.add(getResourceStore(resourceTransaction.getPath()).getChildStore(OVERRIDES_STORE_NAME).openWriteTransaction(resourceTransaction.getResourceId()));
                break;
            case ADD:
            case UPDATE:
            default:
                storeTransactions.add(getResourceStore(resourceTransaction.getPath()).openWriteTransaction(resourceTransaction.getResourceId()));
                break;
        }

        AbstractCacheTransaction<T> cacheTransaction;
        switch (resourceTransaction.getOperation()) {
            case DELETE:
            case DELETE_ALL:
                cacheTransaction = (AbstractCacheTransaction<T>) getResourceCache(resourceTransaction.getPath()).openDeleteTransaction(resourceTransaction.getResourceId());
                break;
            case ADD:
            case UPDATE:
            case UPDATE_OVERRIDE:
            default:
                cacheTransaction = (AbstractCacheTransaction<T>) getResourceCache(resourceTransaction.getPath()).openWriteTransaction(resourceTransaction.getResourceId());
                break;
        }
        cacheTransaction.setOperation(resourceTransaction.getOperation());
        cacheTransaction.setDeepUpdater(deepUpdater);

        resourceTransaction.addTransactions(storeTransactions);
        resourceTransaction.addTransaction(cacheTransaction);
        
        return resourceTransaction;
    }

    protected void executeTransaction(ResourceTransaction<T> resourceTransaction) throws IOException {

        try {
            resourceTransaction.execute();
            
            LOGGER.getLogger().debug("COMMIT SRT");
            
            resourceTransaction.commit();
            
            LOGGER.getLogger().debug("COMMITTED SRT");
            
        } catch (IOException | WriteError ex) {
            resourceTransaction.rollback();

            throw ex;
        } finally {
            resourceTransaction.close();
        }
    }
    
    @Override
    public List<String> getResourceIds() {
        return getPathProxy(this, defaultPath).getResourceIds();
    }

    @Override
    public T getResource(String id) {
        return (T) getPathProxy(this, defaultPath).getResource(id);
    }

    @Override
    public boolean hasResource(String id) {
        return getPathProxy(this, defaultPath).hasResource(id);
    }

    @Override
    public void addResource(T resource) throws IOException {
        getPathProxy(this, defaultPath).addResource(resource);
    }

    @Override
    public void deleteResource(String id) throws IOException {
        getPathProxy(this, defaultPath).deleteResource(id);
    }

    @Override
    public void updateResource(T resource) throws IOException {
        getPathProxy(this, defaultPath).updateResource(resource);
    }

    @Override
    public void updateResourceOverrides(String id, T resource) throws IOException {
        getPathProxy(this, defaultPath).updateResourceOverrides(id, resource);
    }

    @Override
    public U withParent(String storeId) {
        String[] path = {storeId, resourceType};
        return getPathProxy(this, path);
    }

    @Override
    public U withChild(String storeId) {
        String[] path = {resourceType, storeId};
        return getPathProxy(this, path);
    }

    @Override
    public List<String[]> getAllPaths() {
        List<String[]> paths = new ArrayList<>();

        for (String path : getAllChildPaths(rootConfigStore, "")) {
            if (path.contains(resourceType)) {
                paths.add(getPathArray(path));
            }
        }

        return paths;
    }

    private List<String> getAllChildPaths(KeyValueStore store, String parent) {
        List<String> paths = new ArrayList<>();

        for (String child : store.getChildStoreIds()) {
            String path = parent + "/" + child;
            paths.add(path);
            paths.addAll(getAllChildPaths(store.getChildStore(child), path));
        }

        return paths;
    }

    private U getPathProxy(final AbstractGenericResourceStore store, final String[] path) {
        //LOGGER.getLogger().debug("PROXY {} {}", path, proxies.size());
        String p = Joiner.on('/').join(path);
        if (!proxies.containsKey(p)) {
            ProxyAdapter proxy = new ProxyAdapter(store, path);
            proxies.put(p, proxy);
        }

        return (U) proxies.get(p).getProxy();
    }

    private class ProxyAdapter implements ResourceStore<T> {

        private final Object proxy;
        private final AbstractGenericResourceStore store;
        private final String[] path;

        @SuppressWarnings("unchecked")
        public ProxyAdapter(final AbstractGenericResourceStore store, final String[] path) {
            this.store = store;
            this.path = path;
            //Class[] interfaces = ObjectArrays.concat(store.getClass().getInterfaces(), ResourceStore.class);
            Class[] interfaces = store.getClass().getInterfaces();
            //LOGGER.getLogger().debug("PROXY {} {}", path, (Object)interfaces);

            this.proxy = Proxy.newProxyInstance(
                    store.getClass().getClassLoader(),
                    interfaces,
                    new InvocationHandler() {

                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            /*Method m;
                             try {
                             //Determine if the method has been defined in a subclass
                             m = ProxyAdapter.this.getClass().getMethod(method.getName(), method.getParameterTypes());
                             m.setAccessible(true);
                             } catch (Exception e) { //if not found
                             throw new UnsupportedOperationException(method.toString(), e);

                             }
                             */
                            //Invoke the method found and return the result
                            try {
                                if (method.getDeclaringClass() == ResourceStore.class) {
                                    return method.invoke(ProxyAdapter.this, args);
                                } else {
                                    return method.invoke(store, args);
                                }

                            } catch (InvocationTargetException e) {
                                throw e.getCause();
                            }
                        }
                    });
        }

        /**
         * @return proxy instance implementing T.
         */
        public Object getProxy() {
            return proxy;
        }

        @Override
        public List<String> getResourceIds() {
            return AbstractGenericResourceStore.this.getResourceIds(path);
        }

        @Override
        public T getResource(String id) {
            return AbstractGenericResourceStore.this.getResource(path, id);
        }

        @Override
        public boolean hasResource(String id) {
            return AbstractGenericResourceStore.this.hasResource(path, id);
        }

        @Override
        public void addResource(T resource) throws IOException {
            AbstractGenericResourceStore.this.writeResource(path, resource.getResourceId(), OPERATION.ADD, resource);
        }

        @Override
        public void deleteResource(String id) throws IOException {
            AbstractGenericResourceStore.this.writeResource(path, id, OPERATION.DELETE);
        }

        @Override
        public void updateResource(T resource) throws IOException {
            AbstractGenericResourceStore.this.writeResource(path, resource.getResourceId(), OPERATION.UPDATE, resource);
        }

        @Override
        public void updateResourceOverrides(String id, T resource) throws IOException {
            AbstractGenericResourceStore.this.writeResource(path, id, OPERATION.UPDATE_OVERRIDE, resource);
        }

        @Override
        public ResourceStore<T> withParent(String storeId) {
            List<String> newPath = new ArrayList<>(Arrays.asList(path));
            newPath.add(0, storeId);
            return getPathProxy(store, newPath.toArray(new String[newPath.size()]));
        }

        @Override
        public ResourceStore<T> withChild(String storeId) {
            List<String> newPath = new ArrayList<>(Arrays.asList(path));
            newPath.add(storeId);
            return getPathProxy(store, newPath.toArray(new String[newPath.size()]));
        }

        @Override
        public List<String[]> getAllPaths() {
            return AbstractGenericResourceStore.this.getAllPaths();
        }
    }
}
