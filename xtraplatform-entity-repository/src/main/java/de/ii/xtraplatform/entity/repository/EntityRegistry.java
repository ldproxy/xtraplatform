/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entity.repository;

import de.ii.xtraplatform.entity.api.EntityRepository;
import de.ii.xtraplatform.entity.api.PersistentEntity;
import de.ii.xtraplatform.service.api.ImmutableServiceData;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zahnen
 */
@Component(publicFactory = false)
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.xtraplatform.entity.api.PersistentEntity)",
        onArrival = "onStoreArrival",
        onDeparture = "onStoreDeparture")
public class EntityRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityRegistry.class);

    @Context
    BundleContext context;

    @Requires
    EntityRepository entityStore;

    ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);

    private synchronized void onStoreArrival(ServiceReference<PersistentEntity> ref) {
        try {
            final ServiceTest entity = (ServiceTest) context.getService(ref);

            LOGGER.debug("ENTITY {}", entity);
            if (entity != null && entity.getData() != null) {
                LOGGER.debug("ENTITY {} {} {}", entity.getId(), entity.getData()
                                                                              .getId(), entity.getData().getLabel());

                //LOGGER.debug("ENTITY STORE {}", entityStore);

                executorService.schedule(() -> {
                    try {
                        entityStore.replaceEntity(ImmutableServiceData.builder().id("foo").label("Foobar").build());

                        LOGGER.debug("REPLACED");
                        // TODO: not replaced yet, so how to return data on update? by really returning the data from replaceEntity and not the entity itself?
                        // TODO: another point: retracting the instances for updates will not work in every case as state is lost
                    } catch (IOException e) {
                        //ignore
                    }
                }, 5, TimeUnit.SECONDS);
            }
        } catch (Throwable e) {
            LOGGER.error("E", e);
        }
    }

    private synchronized void onStoreDeparture(ServiceReference<PersistentEntity> ref) {
        final ServiceTest entity = (ServiceTest) context.getService(ref);

        LOGGER.debug("ENTITY REMOVED {}", entity != null ? entity.getId() : null);
    }
}
