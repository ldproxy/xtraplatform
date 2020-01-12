/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.event.store;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.entity.api.EntityData;
import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.extender.ConfigurationBuilder;
import org.apache.felix.ipojo.extender.DeclarationBuilderService;
import org.apache.felix.ipojo.extender.DeclarationHandle;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.apache.felix.ipojo.whiteboard.Whiteboards;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zahnen
 */

@Component(publicFactory = false)
@Provides
@Instantiate
@Whiteboards(whiteboards = {
        @Wbp(
                filter = "(&(objectClass=org.apache.felix.ipojo.Factory)(component.providedServiceSpecifications=de.ii.xtraplatform.entity.api.PersistentEntity))",
                onArrival = "onFactoryArrival",
                onDeparture = "onFactoryDeparture"),
        @Wbp(
                filter = "(objectClass=de.ii.xtraplatform.event.store.EntityHydrator)",
                onArrival = "onHydratorArrival",
                onDeparture = "onHydratorDeparture")
})

public class EntityFactoryDefault implements EntityFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityFactoryDefault.class);

    @Context
    private BundleContext context;

    private final DeclarationBuilderService declarationBuilderService;
    private final Map<String, DeclarationHandle> instanceHandles;
    private final Map<String, Factory> componentFactories;
    private final Map<String, String> entityClasses;
    private final Map<Class<?>, String> entityDataTypes;
    private final Map<String, Class<EntityDataBuilder<EntityData>>> entityDataBuilders;
    private final Map<String, EntityHydrator<EntityData>> entityHydrators;

    protected EntityFactoryDefault(@Requires DeclarationBuilderService declarationBuilderService) {
        this.declarationBuilderService = declarationBuilderService;
        this.instanceHandles = new ConcurrentHashMap<>();
        this.componentFactories = new ConcurrentHashMap<>();
        this.entityClasses = new ConcurrentHashMap<>();
        this.entityDataTypes = new ConcurrentHashMap<>();
        this.entityDataBuilders = new ConcurrentHashMap<>();
        this.entityHydrators = new ConcurrentHashMap<>();
    }

    private synchronized void onFactoryArrival(ServiceReference<ComponentFactory> ref) {
        Optional<String> entityClassName = Optional.ofNullable((String) ref.getProperty("component.class"));
        Optional<String> entityType = Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties"))
                                            .filter(pd -> pd.getName()
                                                            .equals("type"))
                                            .map(PropertyDescription::getValue)
                                            .findFirst();
        Optional<String> entityDataType = Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties"))
                                                .filter(pd -> pd.getName()
                                                                .equals("data"))
                                                .map(PropertyDescription::getType)
                                                .findFirst();
        Optional<String> entityDataGenerators = Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties"))
                                                      .filter(pd -> pd.getName()
                                                                      .equals("generators"))
                                                      .map(PropertyDescription::getType)
                                                      .findFirst();

        if (entityClassName.isPresent() && entityDataType.isPresent() && entityType.isPresent()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("ENTITY FACTORY {} {} {}", entityDataType.get(), entityClassName.get(), entityType.get());
            }
            ComponentFactory factory = context.getService(ref);
            try {
                Class entityClass = factory.loadClass(factory.getClassName());
                Class entityDataClass = factory.loadClass(entityDataType.get());
                JsonDeserialize annotation = (JsonDeserialize) entityDataClass.getAnnotation(JsonDeserialize.class);
                Class<EntityDataBuilder<EntityData>> builder = (Class<EntityDataBuilder<EntityData>>) annotation.builder();

                this.entityDataBuilders.put(entityType.get(), builder);
                this.entityDataTypes.put(entityDataClass, entityType.get());
                //TODO
                if (Objects.nonNull(entityDataClass.getSuperclass())) {
                    Arrays.stream(entityDataClass.getSuperclass()
                                                 .getInterfaces())
                          .forEach(c -> {
                              if (c.getSimpleName()
                                   .endsWith("ServiceData")) {
                                  this.entityDataTypes.put(c, entityType.get());
                              }
                          });
                }

                boolean br = true;
            } catch (ClassNotFoundException e) {

            }

            this.componentFactories.put(entityType.get(), factory);
            this.entityClasses.put(entityType.get(), entityClassName.get());
        }


    }

    private synchronized void onFactoryDeparture(ServiceReference<Factory> ref) {
        Optional<String> entityType = Optional.ofNullable((String) ref.getProperty("component.class"));
        Optional<String> entityDataType = Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties"))
                                                .filter(pd -> pd.getName()
                                                                .equals("data"))
                                                .map(PropertyDescription::getType)
                                                .findFirst();

        if (entityType.isPresent() && entityDataType.isPresent()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("REMOVE ENTITY FACTORY {} {}", entityDataType.get(), entityType.get());
            }
            this.componentFactories.remove(entityDataType.get());
            this.entityClasses.remove(entityDataType.get());

        }
    }

    private synchronized void onHydratorArrival(ServiceReference<EntityHydrator<EntityData>> ref) {
        Optional<String> entityType = Optional.ofNullable((String) ref.getProperty("entityType"));

        if (entityType.isPresent()) {
            EntityHydrator<EntityData> entityHydrator = context.getService(ref);
            this.entityHydrators.put(entityType.get(), entityHydrator);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("ENTITY HYDRATOR {} {}", entityType.get(), entityHydrator);
            }
        }
    }

    private synchronized void onHydratorDeparture(ServiceReference<EntityHydrator<EntityData>> ref) {
        try {
            Optional<String> entityType = Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties"))
                                                .filter(pd -> pd.getName()
                                                                .equals("entityType"))
                                                .map(PropertyDescription::getValue)
                                                .findFirst();

            if (entityType.isPresent()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("REMOVE ENTITY HYDRATOR {}", entityType.get());
                }
                this.entityHydrators.remove(entityType.get());
            }
        } catch (Throwable w) {
            //ignore
        }
    }

    @Override
    public EntityDataBuilder<EntityData> getDataBuilder(String entityType) {

        try {
            return entityDataBuilders.get(entityType)
                                     .newInstance();
        } catch (Throwable e) {
            throw new IllegalStateException("no builder found for entity type " + entityType);
        }
    }

    @Override
    public String getDataTypeName(Class<? extends EntityData> entityDataClass) {
        return entityDataTypes.get(entityDataClass);
    }

    @Override
    public void createInstance(String entityType, String id, EntityData entityData) {

        LOGGER.debug("CREATING ENTITY {} {} {}", entityType, id/*, entityData*/);

        String instanceId = entityType + "/" + id;
        String instanceClassName = entityClasses.get(entityType);

        ConfigurationBuilder instanceBuilder = declarationBuilderService.newInstance(instanceClassName)
                                                                        .name(instanceId)
                                                                        .configure()
                                                                        .property("data", entityData);

        if (entityHydrators.containsKey(entityType)) {
            try {
                entityHydrators.get(entityType)
                               .getInstanceConfiguration(entityData)
                               .forEach(instanceBuilder::property);
            } catch (Throwable e) {
                LOGGER.error("Entity of type '{}' with id '{}' could not be hydrated: {}", entityType, id, e.getMessage());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Cause:", e);
                }
                throw e;
            }

        }

        DeclarationHandle handle = instanceBuilder.build();

        handle.publish();

        this.instanceHandles.put(instanceId, handle);
    }

    @Override
    public void updateInstance(String entityType, String id, EntityData entityData) {
        LOGGER.debug("UPDATING ENTITY {} {} {}", entityType, id/*, entityData*/);

        String instanceId = entityType + "/" + id;

        deleteInstance(entityType, id);
        createInstance(entityType, id, entityData);

        /*if (instanceHandles.containsKey(instanceId)) {
            Dictionary<String, Object> configuration = new Hashtable<>();
            configuration.put("instance.name", instanceId);
            configuration.put("data", entityData);

            if (entityHydrators.containsKey(entityType)) {
                entityHydrators.get(entityType)
                               .getInstanceConfiguration(entityData)
                               .forEach(configuration::put);
            }

            try {
                componentFactories.get(entityType)
                                  .reconfigure(configuration);
            } catch (Throwable e) {
                //ignore
                LOGGER.error("ERROR UPDATING", e);
            }
        }*/
    }

    @Override
    public void deleteInstance(String entityType, String id) {
        LOGGER.debug("DELETING ENTITY {} {}", entityType, id);

        String instanceId = entityType + "/" + id;

        if (instanceHandles.containsKey(instanceId)) {
            instanceHandles.get(instanceId)
                           .retract();
            instanceHandles.remove(instanceId);
        }
    }
}
