/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entity.repository;

import de.ii.xtraplatform.entity.api.EntityData;
import de.ii.xtraplatform.entity.api.EntityRepository;
import de.ii.xtraplatform.entity.api.EntityRepositoryForType;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.extender.DeclarationBuilderService;
import org.apache.felix.ipojo.extender.DeclarationHandle;
import org.apache.felix.ipojo.extender.InstanceBuilder;
import org.apache.felix.ipojo.handlers.event.Subscriber;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zahnen
 */

/*@Component(publicFactory = false)
@Provides(specifications = {EntityInstantiator.class})
@Instantiate
@Wbp(
        filter = "(&(objectClass=org.apache.felix.ipojo.Factory)(component.providedServiceSpecifications=de.ii.xtraplatform.entity.api.PersistentEntity))",
        onArrival = "onFactoryArrival",
        onDeparture = "onFactoryDeparture")

 */
public class EntityInstantiator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityInstantiator.class);

    @Context
    private BundleContext context;

    @Requires
    private DeclarationBuilderService declarationBuilderService;

    //@Requires
    //JacksonSubTypeIds[] jacksonSubTypeIds;

    ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);

    private final EntityRepository entityRepository;
    private final Map<String, DeclarationHandle> instanceHandles;
    private final Map<String, Factory> componentFactories;
    private final Map<String, String> entityClasses;
    private final Map<String, String> entityTypes;
    private final Map<String, EntityData> entityBuffer;

    public EntityInstantiator(@Requires EntityRepository entityRepository) {
        this.instanceHandles = new LinkedHashMap<>();
        this.componentFactories = new LinkedHashMap<>();
        this.entityClasses = new LinkedHashMap<>();
        this.entityTypes = new LinkedHashMap<>();
        this.entityBuffer = new LinkedHashMap<>();
        this.entityRepository = entityRepository;
        //entityRepository.addChangeListener(this);
    }

    public String getResult() {
        return "It's so hot!";
    }

    private synchronized void onFactoryArrival(ServiceReference<Factory> ref) {
        Optional<String> entityClass = Optional.ofNullable((String)ref.getProperty("component.class"));
        Optional<String> entityType = Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties"))
                                            .filter(pd -> pd.getName().equals("type"))
                                            .map(PropertyDescription::getValue)
                                            .findFirst();
        Optional<String> entityDataType = Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties"))
                                                .filter(pd -> pd.getName().equals("data"))
                                                .map(PropertyDescription::getType)
                                                .findFirst();
        Optional<String> entityDataGenerators = Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties"))
                                                .filter(pd -> pd.getName().equals("generators"))
                                                .map(PropertyDescription::getType)
                                                .findFirst();

        if (entityClass.isPresent() && entityDataType.isPresent() && entityType.isPresent()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("ENTITY FACTORY {} {} {}", entityDataType.get(), entityClass.get(), entityType.get());
            }
            this.componentFactories.put(entityDataType.get(), context.getService(ref));
            this.entityClasses.put(entityDataType.get(), entityClass.get());
            this.entityTypes.put(entityDataType.get(), entityType.get());

            entityRepository.addEntityType(entityType.get(), entityDataType.get());

            //LOGGER.debug("JACKSON SUBTYPES {}", jacksonSubTypeIds);
            executorService.schedule(() -> initInstances(entityType.get(), entityClass.get()), 10, TimeUnit.SECONDS);
        }


    }

    private synchronized void onFactoryDeparture(ServiceReference<Factory> ref) {
        Optional<String> entityType = Optional.ofNullable((String)ref.getProperty("component.class"));
        Optional<String> entityDataType = Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties"))
                                                .filter(pd -> pd.getName().equals("data"))
                                                .map(PropertyDescription::getType)
                                                .findFirst();

        if (entityType.isPresent() && entityDataType.isPresent()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("REMOVE ENTITY FACTORY {} {}", entityDataType.get(), entityType.get());
            }
            this.componentFactories.remove(entityDataType.get());
            this.entityClasses.remove(entityDataType.get());

            clearInstances(entityType.get());
        }
    }

    private Optional<String> getEntityClass(EntityData data) {
        return Optional.ofNullable(entityClasses.get(getEntityDataType(data)));
    }

    private String getEntityDataType(EntityData data) {
        return data.getClass().getName().replace(".Immutable", ".").replace(".Modifiable", ".");
    }

    @Subscriber(name = "create", topics = "create", dataKey = "data", dataType = "de.ii.xtraplatform.entity.api.AbstractEntityData")
    public void onEntityCreate(EntityData data) {
        LOGGER.debug("TYPES {} {}", entityClasses, data.getClass().getName().replace(".Immutable", "."));
        Optional<String> entityType = getEntityClass(data);
        if (entityType.isPresent()) {
            createInstance(entityType.get(), data.getId(), data);
        } else {
            entityBuffer.put(data.getClass().getName(), data);
        }
    }

    @Subscriber(name = "update", topics = "update", dataKey = "data", dataType = "de.ii.xtraplatform.entity.api.AbstractEntityData")
    public void onEntityUpdate(EntityData data) {
        updateInstance(getEntityDataType(data), data.getId(), data);
    }

    @Subscriber(name = "delete", topics = "delete", dataKey = "data", dataType = "java.lang.String")
    public void onEntityDelete(String id) {
        deleteInstance(id);
    }

    private void initInstances(String type, String clazz) {
        EntityRepositoryForType entityRepositoryForType = new EntityRepositoryForType(entityRepository, type);
        LOGGER.debug("INIT ENTITIES {} {}", type, entityRepositoryForType.getEntityIds());

        for (String id : entityRepositoryForType.getEntityIds()) {
            createInstance(clazz, id, entityRepositoryForType.getEntityData(id));
        }
    }

    private void clearInstances(String type) {
        LOGGER.debug("CLEAR ENTITIES {}", type);

        for (String id : instanceHandles.keySet()) {
            if (id.startsWith(type + "/")) {
                deleteInstance(id);
            }
        }
    }

    private void createInstance(String type, String id, EntityData data) {
        LOGGER.debug("CREATE ENTITY {} {} {}", type, id/*, data*/);

        InstanceBuilder instanceBuilder = declarationBuilderService.newInstance(type);

        // TODO: type/id
        DeclarationHandle handle = instanceBuilder.name(type+ "/" + id)
                                                  .configure()
                                                  // simulate deserialization to Modifiable
                                                  //.property("data", ModifiableServiceData.create().from((ServiceData) data))
                                                  .property("data", data)
                                                  .property("organization", "ORG")
                                                  .build();

        handle.publish();

        this.instanceHandles.put(id, handle);
    }

    private void updateInstance(String type, String id, EntityData data) {
        LOGGER.debug("UPDATE ENTITY {} {} {}", type, id/*, data*/);

        if (componentFactories.containsKey(type) && instanceHandles.containsKey(id)) {
            Dictionary<String, Object> configuration = new Hashtable<>();
            configuration.put("instance.name", getEntityClass(data).get() + '/' + id);
            configuration.put("data", data);

            try {
                componentFactories.get(type)
                                  .reconfigure(configuration);
            } catch (Throwable e) {
                //ignore
                LOGGER.error("ERROR UPDATING", e);
            }
        }
    }

    private void deleteInstance(String id) {
        LOGGER.debug("DELETE ENTITY {}", id);

        if (instanceHandles.containsKey(id)) {
            instanceHandles.get(id)
                           .retract();
            instanceHandles.remove(id);
        }
    }
}
