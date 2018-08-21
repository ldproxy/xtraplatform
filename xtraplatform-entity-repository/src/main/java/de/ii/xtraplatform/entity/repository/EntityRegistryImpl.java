/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entity.repository;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.entity.api.EntityRegistry;
import de.ii.xtraplatform.entity.api.EntityRepository;
import de.ii.xtraplatform.entity.api.PersistentEntity;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@Component(publicFactory = false)
@Provides
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.xtraplatform.entity.api.PersistentEntity)",
        onArrival = "onStoreArrival",
        onDeparture = "onStoreDeparture")
public class EntityRegistryImpl implements EntityRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityRegistryImpl.class);

    @Context
    BundleContext context;

    @Requires
    EntityRepository entityStore;

    ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);

    private final Map<String, Map<String, PersistentEntity>> entities;

    public EntityRegistryImpl() {
        this.entities = new LinkedHashMap<>();
    }

    private synchronized void onStoreArrival(ServiceReference<PersistentEntity> ref) {
        try {
            final PersistentEntity entity = context.getService(ref);

            LOGGER.debug("ENTITY {}", entity);
            if (entity != null && entity.getData() != null) {
                LOGGER.debug("ENTITY {} {} {}", entity.getType(), entity.getId()/*, entity.getData()*/);

                //LOGGER.debug("ENTITY STORE {}", entityStore);

                registerEntity(entity);
            }
        } catch (Throwable e) {
            LOGGER.error("E", e);
        }
    }

    private synchronized void onStoreDeparture(ServiceReference<PersistentEntity> ref) {
        final PersistentEntity entity = context.getService(ref);

        LOGGER.debug("ENTITY REMOVED {}", entity != null ? entity.getId() : null);
    }

    private void registerEntity(PersistentEntity entity) {
        if (!entities.containsKey(entity.getType())) {
            entities.put(entity.getType(), new LinkedHashMap<>());
        }
        entities.get(entity.getType()).put(entity.getId(), entity);
    }

    private void deregisterEntity(PersistentEntity entity) {
        if (Objects.isNull(entity) || !entities.containsKey(entity.getType())) {
            return;
        }
        entities.get(entity.getType()).remove(entity.getId());
    }

    @Override
    public <T extends PersistentEntity> List<T> getEntitiesForType(Class<T> clazz, String type) {
        return entities.containsKey(type) ? ImmutableList.copyOf((Iterable<? extends T>) entities.get(type).values()) : ImmutableList.of();
    }

    @Override
    public <T extends PersistentEntity> Optional<T> getEntity(Class<T> clazz, String type, String id) {
        return entities.containsKey(type) ? Optional.ofNullable((T)entities.get(type).get(id)) : Optional.empty();
    }

    /*@Override
    public List<PersistentEntity> getEntitiesForType(Class<T> clazz, String type) {
        return entities.containsKey(type) ? ImmutableList.copyOf(entities.get(type).values()) : ImmutableList.of();
    }

    @Override
    public Optional<PersistentEntity> getEntity(Class<T> clazz, String type, String id) {
        return entities.containsKey(type) ? Optional.ofNullable(entities.get(type).get(id)) : Optional.empty();
    }*/
}
