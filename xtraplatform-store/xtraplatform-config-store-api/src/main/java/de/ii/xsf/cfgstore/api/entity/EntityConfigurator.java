/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.cfgstore.api.entity;

import de.ii.xsf.configstore.api.rest.ResourceStore;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author zahnen
 */
@Component(publicFactory = false)
@Provides
//@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.xsf.configstore.api.rest.ResourceStore)",
        onArrival = "onStoreArrival",
        onDeparture = "onStoreDeparture")
public class EntityConfigurator implements EntityStore<EntityConfigurationTestImpl, PartialEntityConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityConfigurator.class);

    @Context
    BundleContext context;

    @Requires
    DeclarationBuilderService declarationBuilderService;

    @Requires
    private Factory[] factories;

    final Map<String, ResourceStore> stores;
    final Map<String, DeclarationHandle> instanceHandles;

    public EntityConfigurator() {
        this.stores = new HashMap<>();
        this.instanceHandles = new HashMap<>();
    }

    ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);

    private synchronized void onStoreArrival(ServiceReference<ResourceStore<Entity>> ref) {
        LOGGER.debug("ARRIVAL {}", ref.getProperty("cfgForEntity"));
        final String entityType = (String) ref.getProperty("cfgForEntity");
        final ResourceStore<Entity> store = context.getService(ref);

        addStore(entityType, store);
        LOGGER.debug("STORES {}", stores);
    }

    private synchronized void onStoreDeparture(ServiceReference<ResourceStore> ref) {
        // TODO: retract handles
        Optional.ofNullable(context.getService(ref))
                .ifPresent(resourceStore -> stores.remove(resourceStore.getClass()
                                                                       .getName()));
    }

    private void addStore(final String entityType, final ResourceStore<Entity> store) {
        if (Objects.nonNull(entityType) && Objects.nonNull(store)) {
            stores.put(entityType, store);

            String componentId = Instant.now().toString();
            EntityConfigurationTestImpl configuration = new EntityConfigurationTestImpl("foo", "abc");
            Entity entity = new EntityTestImpl(componentId, configuration);

            /*try {
                store.addResource(entity);
            } catch (IOException e) {
                //ignore
            }*/


            DeclarationHandle instance = createInstance(entityType, componentId, configuration, true);

            this.instanceHandles.put(componentId, instance);
        }
    }

    @Validate
    void onStart() {
        /*String componentId ="xyz";

        DeclarationHandle instance = createInstance(TestConfigurableComponent.class, componentId, new EntityConfiguration("foo"), true);

        executorService.schedule(() -> {
            LOGGER.debug("STATUS {} {} {} {}", componentId, instance.getStatus().getMessage(), instance.getStatus().getThrowable(), instance.getStatus().isBound());

            instance.retract();

            DeclarationHandle instance2 = createInstance(TestConfigurableComponent.class, componentId, new EntityConfiguration("bar"), false);

            instance2.retract();

            createInstance(TestConfigurableComponent.class, componentId, new EntityConfiguration("foobar"), true);
        }, 0, TimeUnit.SECONDS);*/
    }

    private DeclarationHandle createInstance(String entityType, String componentId, Object value, Boolean online) {
        InstanceBuilder instanceBuilder = declarationBuilderService.newInstance(entityType);

        LOGGER.debug("CREATING {} {}", entityType, componentId);

        DeclarationHandle handle = instanceBuilder.name(entityType + "-" + componentId)
                                                  .configure()
                                                  .property("id", componentId)
                                                  .property("value", value)
                                                  .property("online", online)
                                                  .property("organization", "ORG")
                                                  .build();

        handle.publish();

        return handle;
    }

    @Override
    public EntityConfigurationTestImpl getEntityData(String id) {
        if (instanceHandles.containsKey(id)) {
            //return instanceHandles.get(id);
        }
        return null;
    }

    @Override
    public boolean hasEntity(String id) {
        return instanceHandles.containsKey(id);
    }

    @Override
    public void createEntity(String id, EntityConfigurationTestImpl data) throws IOException {
        DeclarationHandle instance = createInstance("de.ii.xsf.cfgstore.api.entity.EntityTestImpl", id, data, true);

        this.instanceHandles.put(id, instance);
    }

    @Override
    public void replaceEntity(String id, EntityConfigurationTestImpl data) throws IOException {

        if (instanceHandles.containsKey(id)) {
            //instanceHandles.get(id).retract();
        }

        //DeclarationHandle instance = createInstance("de.ii.xsf.cfgstore.api.fromstore.handler.EntityTestImpl", id, data, true);

        //this.instanceHandles.put(id, instance);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("instance.name", "de.ii.xsf.cfgstore.api.entity.EntityTestImpl-" + id);
        configuration.put("value", data);

        for (Factory factory : factories) {
            if ( factory.getName().equals("de.ii.xsf.cfgstore.api.entity.EntityTestImpl")) {
                LOGGER.debug("FACTORY {}", Arrays.asList(factory.getComponentDescription().getprovidedServiceSpecification()));
                try {
                    factory.reconfigure(configuration);
                } catch (UnacceptableConfiguration | MissingHandlerException unacceptableConfiguration) {
                    //ignore
                }
                break;
            }
        }

    }

    @Override
    public void updateEntity(String id, PartialEntityConfiguration partialData) throws IOException {

    }

    @Override
    public void deleteEntity(String id) throws IOException {
        if (instanceHandles.containsKey(id)) {
            instanceHandles.get(id).retract();
            instanceHandles.remove(id);
        }
    }
}
