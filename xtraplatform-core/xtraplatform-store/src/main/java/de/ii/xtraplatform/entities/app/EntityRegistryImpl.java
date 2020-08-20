/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.app;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.entities.domain.EntityRegistry;
import de.ii.xtraplatform.entities.domain.PersistentEntity;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * @author zahnen
 */
@Component(publicFactory = false)
@Provides
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.xtraplatform.entities.domain.PersistentEntity)",
        onArrival = "onEntityArrival",
        onDeparture = "onEntityDeparture")
public class EntityRegistryImpl implements EntityRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityRegistryImpl.class);

    private final BundleContext context;
    private final Set<PersistentEntity> entities;
    private final List<BiConsumer<String, PersistentEntity>> entityListeners;

    public EntityRegistryImpl(@Context BundleContext context) {
        this.context = context;
        this.entities = new HashSet<>();
        this.entityListeners = new ArrayList<>();
    }

    private synchronized void onEntityArrival(ServiceReference<PersistentEntity> ref) {
        try {
            final PersistentEntity entity = context.getService(ref);

            if (Objects.nonNull(entity)) {
                entities.add(entity);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Registered entity: {} {}", entity.getClass(), entity.getId());
                }

                String instanceId = (String) ref.getProperty("instance.name");
                entityListeners.forEach(listener -> listener.accept(instanceId, entity));
            }
        } catch (Throwable e) {
            LOGGER.error("E", e);
        }
    }

    private synchronized void onEntityDeparture(ServiceReference<PersistentEntity> ref) {
        final PersistentEntity entity = context.getService(ref);

        if (Objects.nonNull(entity)) {
            entities.remove(entity);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Deregistered entity: {} {}", entity.getClass(), entity.getId());
            }
        }

        LOGGER.debug("ENTITY REMOVED {}", entity != null ? entity.getId() : null);
    }

    @Override
    public <T extends PersistentEntity> List<T> getEntitiesForType(Class<T> type) {
        return (List<T>) entities.stream()
                       .filter(persistentEntity -> type.isAssignableFrom(persistentEntity.getClass()))
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public <T extends PersistentEntity> Optional<T> getEntity(Class<T> type, String id) {
        return (Optional<T>) entities.stream()
                                     .filter(persistentEntity -> type.isAssignableFrom(persistentEntity.getClass()) && persistentEntity.getId()
                                                                                                                                       .equals(id))
                                     .findFirst();
    }

    @Override
    public void addEntityListener(BiConsumer<String, PersistentEntity> listener) {
        this.entityListeners.add(listener);
        //entities.forEach(entity -> listener.accept(entity.getId(), entity));
    }
}
