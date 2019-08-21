/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.entity.api.EntityData;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.extender.ConfigurationBuilder;
import org.apache.felix.ipojo.extender.DeclarationBuilderService;
import org.apache.felix.ipojo.extender.DeclarationHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zahnen
 */

public abstract class AbstractEntityInstantiator<T extends EntityData> implements EntityInstantiator<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEntityInstantiator.class);

    private final DeclarationBuilderService declarationBuilderService;
    private final Map<String, DeclarationHandle> instanceHandles;
    private final Factory componentFactory;

    public AbstractEntityInstantiator(DeclarationBuilderService declarationBuilderService, Factory componentFactory) {
        this.declarationBuilderService = declarationBuilderService;
        this.componentFactory = componentFactory;
        this.instanceHandles = new ConcurrentHashMap<>();
    }

    protected abstract Map<String, Object> getInstanceConfiguration(T data);

    @Override
    public void createInstance(String type, String id, T data) {
        LOGGER.debug("CREATE ENTITY {} {} {}", type, id/*, data*/);

        ConfigurationBuilder instanceBuilder = declarationBuilderService.newInstance(type)
                                                                        .name(type + "/" + id)
                                                                        .configure()
                                                                        .property("data", data);

        getInstanceConfiguration(data).forEach(instanceBuilder::property);

        DeclarationHandle handle = instanceBuilder.build();

        handle.publish();

        this.instanceHandles.put(id, handle);
    }

    @Override
    public void updateInstance(String type, String id, T data) {
        LOGGER.debug("UPDATE ENTITY {} {} {}", type, id/*, data*/);

        if (instanceHandles.containsKey(id)) {
            Dictionary<String, Object> configuration = new Hashtable<>();
            configuration.put("instance.name", type + '/' + id);
            configuration.put("data", data);

            getInstanceConfiguration(data).forEach(configuration::put);

            try {
                componentFactory.reconfigure(configuration);
            } catch (Throwable e) {
                //ignore
                LOGGER.error("ERROR UPDATING", e);
            }
        }
    }

    @Override
    public void deleteInstance(String id) {
        LOGGER.debug("DELETE ENTITY {}", id);

        if (instanceHandles.containsKey(id)) {
            instanceHandles.get(id)
                           .retract();
            instanceHandles.remove(id);
        }
    }
}
