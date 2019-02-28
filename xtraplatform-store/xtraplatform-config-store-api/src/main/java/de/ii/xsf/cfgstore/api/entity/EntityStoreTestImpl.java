/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.cfgstore.api.entity;

import de.ii.xsf.configstore.api.KeyValueStore;
import de.ii.xsf.dropwizard.api.Jackson;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.extender.DeclarationBuilderService;
import org.apache.felix.ipojo.extender.DeclarationHandle;
import org.apache.felix.ipojo.extender.InstanceBuilder;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@Component
@Provides(properties = {@StaticServiceProperty(name = "cfgForEntity", value = "de.ii.xsf.cfgstore.api.entity.EntityTestImpl", type = "java.lang.String")})
//@Instantiate
@Wbp(
        filter = "(&(objectClass=org.apache.felix.ipojo.Factory)(component.providedServiceSpecifications=de.ii.xsf.cfgstore.api.entity.Entity))",
        onArrival = "onStoreArrival",
        onDeparture = "onStoreDeparture")
public class EntityStoreTestImpl extends AbstractEntityStore<EntityConfiguration, PartialEntityConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityStoreTestImpl.class);

    public static final String STORE_ID = "test-entity-store";

    @Context
    BundleContext context;

    @Requires
    DeclarationBuilderService declarationBuilderService;

    final Map<String, DeclarationHandle> instanceHandles;
    final Map<String, Factory> componentFactories;

    public EntityStoreTestImpl(@Requires Jackson jackson, @Requires KeyValueStore rootConfigStore) {
        super(rootConfigStore, STORE_ID, jackson.getDefaultObjectMapper());
        this.instanceHandles = new LinkedHashMap<>();
        this.componentFactories = new LinkedHashMap<>();
    }

    private synchronized void onStoreArrival(ServiceReference<Factory> ref) {
        LOGGER.debug("ENTITY FACTORY {} {} {}", ref.getProperty("factory.name"), ref.getProperty("component.class"), Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties")).map(pd -> pd.getName() + ":" + pd.getType()).collect(Collectors.toList()));
        this.componentFactories.put((String) ref.getProperty("factory.name"), context.getService(ref));

        if (getIds().isEmpty()) {
            try {
                super.createEntity("foo", new EntityConfigurationTestImpl("foo", "abc"));
            } catch (IOException e) {

            }
        }

        initInstances((String) ref.getProperty("factory.name"));
    }

    private synchronized void onStoreDeparture(ServiceReference<Factory> ref) {
        LOGGER.debug("REMOVE ENTITY FACTORY {}", ref.getProperty("factory.name"));
        this.componentFactories.remove((String) ref.getProperty("factory.name"));

        clearInstances((String) ref.getProperty("factory.name"));
    }

    @Override
    public void createEntity(String id, EntityConfiguration data) throws IOException {
        super.createEntity(id, data);

        //TODO
        String type = EntityTestImpl.class.getName();
        createInstance(type, id, data);
    }

    @Override
    public void replaceEntity(String id, EntityConfiguration data) throws IOException {
        super.replaceEntity(id, data);

        //TODO
        String type = EntityTestImpl.class.getName();
        updateInstance(type, id, data);
    }

    @Override
    public void updateEntity(String id, PartialEntityConfiguration partialData) throws IOException {
        super.updateEntity(id, partialData);

        //TODO
        String type = EntityTestImpl.class.getName();

        //TODO create merged data
        EntityConfiguration data = null;

        updateInstance(type, id, data);
    }

    @Override
    public void deleteEntity(String id) throws IOException {
        super.deleteEntity(id);

        //TODO
        String type = EntityTestImpl.class.getName();
        deleteInstance(type, id);
    }

    private void initInstances(String type) {
        LOGGER.debug("INIT ENTITIES {} {}", type, getIds());

        for (String id: getIds()) {
            createInstance(type, id, getEntityData(id));
        }

        /*String componentId = Instant.now()
                                    .toString();
        EntityConfigurationTestImpl configuration = new EntityConfigurationTestImpl("foo", "a", "b", "c");

        createInstance(type, componentId, configuration);
        try {
            //createEntity(componentId, configuration);
        } catch (Throwable e) {
            LOGGER.error("E", e);
        }*/
    }

    private void clearInstances(String type) {
        LOGGER.debug("CLEAR ENTITIES {}", type);

        for (String id: getIds()) {
            deleteInstance(type, id);
        }
    }

    private void createInstance(String type, String id, EntityConfiguration data) {
        LOGGER.debug("CREATE ENTITY {}", type, id, data);
try {
    InstanceBuilder instanceBuilder = declarationBuilderService.newInstance(type);

    DeclarationHandle handle = instanceBuilder.name(type + "-" + id)
                                              .configure()
                                              .property("id", id)
                                              .property("data", data)
                                              .property("online", true)
                                              .property("organization", "ORG")
                                              .build();

    handle.publish();

    this.instanceHandles.put(id, handle);
} catch (Throwable e) {
    LOGGER.error("E", e);
}
    }

    private void updateInstance(String type, String id, EntityConfiguration data) {
        LOGGER.debug("UPDATE ENTITY {}", type, id, data);

        if (componentFactories.containsKey(type) && instanceHandles.containsKey(id)) {
            Dictionary<String, Object> configuration = new Hashtable<>();
            configuration.put("instance.name", type + "-" + id);
            configuration.put("data", data);

            try {
                componentFactories.get(type)
                                  .reconfigure(configuration);
            } catch (UnacceptableConfiguration | MissingHandlerException unacceptableConfiguration) {
                //ignore
            }
        }
    }

    private void deleteInstance(String type, String id) {
        LOGGER.debug("DELETE ENTITY {}", type, id);

        if (instanceHandles.containsKey(id)) {
            instanceHandles.get(id)
                           .retract();
            instanceHandles.remove(id);
        }
    }


}
