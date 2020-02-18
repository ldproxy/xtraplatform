/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.event.store;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.entity.api.EntityData;
import de.ii.xtraplatform.entity.api.EntityRegistry;
import de.ii.xtraplatform.entity.api.PersistentEntity;
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
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
                onDeparture = "onHydratorDeparture"),
        @Wbp(
                filter = "(objectClass=de.ii.xtraplatform.event.store.EntityMigration)",
                onArrival = "onMigrationArrival",
                onDeparture = "onMigrationDeparture")
})

public class EntityFactoryDefault implements EntityFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityFactoryDefault.class);

    private final BundleContext context;
    private final DeclarationBuilderService declarationBuilderService;
    private final Map<String, DeclarationHandle> instanceHandles;
    private final Map<String, CompletableFuture<PersistentEntity>> instanceRegistration;
    //private final Map<String, Factory> componentFactories;
    private final Map<String, String> entityClasses;
    private final Map<Class<?>, String> entityDataTypes;
    private final Map<String, Class<EntityDataBuilder<EntityData>>> entityDataBuilders;
    private final Map<String, EntityHydrator<EntityData>> entityHydrators;
    private final Map<String, Map<Long, EntityMigration<EntityData, EntityData>>> entityMigrations;
    private final ScheduledExecutorService executorService;

    protected EntityFactoryDefault(@Context BundleContext context,
                                   @Requires DeclarationBuilderService declarationBuilderService,
                                   @Requires EntityRegistry entityRegistry) {
        this.context = context;
        this.declarationBuilderService = declarationBuilderService;
        this.instanceHandles = new ConcurrentHashMap<>();
        this.instanceRegistration = new ConcurrentHashMap<>();
        //this.componentFactories = new ConcurrentHashMap<>();
        this.entityClasses = new ConcurrentHashMap<>();
        this.entityDataTypes = new ConcurrentHashMap<>();
        this.entityDataBuilders = new ConcurrentHashMap<>();
        this.entityHydrators = new ConcurrentHashMap<>();
        this.entityMigrations = new ConcurrentHashMap<>();
        this.executorService = new ScheduledThreadPoolExecutor(1);

        entityRegistry.addEntityListener((instanceId, entity) -> {
            if (instanceRegistration.containsKey(instanceId)) {
                instanceRegistration.get(instanceId).complete(entity);
            }
        });
    }

    private synchronized void onFactoryArrival(ServiceReference<ComponentFactory> ref) {
        Optional<String> entityClassName = Optional.ofNullable((String) ref.getProperty("component.class"));
        Optional<String> entityType = Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties"))
                                            .filter(pd -> pd.getName()
                                                            .equals("type"))
                                            .map(PropertyDescription::getValue)
                                            .findFirst();
        Optional<String> entitySubType = Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties"))
                                            .filter(pd -> pd.getName()
                                                            .equals("subType"))
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
            String specificEntityType = getSpecificEntityType(entityType.get(), entitySubType);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("ENTITY FACTORY {} {} {}", entityDataType.get(), entityClassName.get(), specificEntityType);
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

            //this.componentFactories.put(type, factory);
            this.entityClasses.put(specificEntityType, entityClassName.get());
        }


    }

    //TODO
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
            //this.componentFactories.remove(entityDataType.get());
            this.entityClasses.remove(entityDataType.get());

        }
    }

    private synchronized void onHydratorArrival(ServiceReference<EntityHydrator<EntityData>> ref) {
        Optional<String> entityType = Optional.ofNullable((String) ref.getProperty("entityType"));
        Optional<String> entitySubType = Optional.ofNullable((String) ref.getProperty("entitySubType"));

        if (entityType.isPresent()) {
            String specificEntityType = getSpecificEntityType(entityType.get(), entitySubType);
            EntityHydrator<EntityData> entityHydrator = context.getService(ref);
            this.entityHydrators.put(specificEntityType, entityHydrator);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("ENTITY HYDRATOR {} {}", specificEntityType, entityHydrator);
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

    private synchronized void onMigrationArrival(ServiceReference<EntityMigration<EntityData, EntityData>> ref) {
        Optional<String> entityType = Optional.ofNullable((String) ref.getProperty("entityType"));

        if (entityType.isPresent()) {
            EntityMigration<EntityData, EntityData> entityMigration = context.getService(ref);
            entityMigrations.putIfAbsent(entityType.get(), new ConcurrentHashMap<>());
            this.entityMigrations.get(entityType.get())
                                 .put(entityMigration.getSourceVersion(), entityMigration);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Registered entity schema migration: {} v{} -> v{}", entityType.get(), entityMigration.getSourceVersion(), entityMigration.getTargetVersion());
            }
        }
    }

    private synchronized void onMigrationDeparture(ServiceReference<EntityMigration<EntityData, EntityData>> ref) {
        try {
            Optional<String> entityType = Optional.ofNullable((String) ref.getProperty("entityType"));

            if (entityType.isPresent()) {
                EntityMigration<EntityData, EntityData> entityMigration = context.getService(ref);
                this.entityMigrations.get(entityType.get())
                                     .remove(entityMigration.getSourceVersion());

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Deregistered entity schema migration: {} v{} -> v{}", entityType.get(), entityMigration.getSourceVersion(), entityMigration.getTargetVersion());
                }
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
    public EntityDataBuilder<EntityData> getDataBuilder(String entityType, long entitySchemaVersion) {
        try {
            return entityMigrations.get(entityType)
                                   .get(entitySchemaVersion)
                                   .getDataBuilder();
        } catch (Throwable e) {
            throw new IllegalStateException("no builder found for entity type " + entityType);
        }
    }

    @Override
    public Map<Identifier, EntityData> migrateSchema(Identifier identifier,
                                                     String entityType, EntityData entityData,
                                                     OptionalLong targetVersion) {
        long sourceVersion = entityData.getEntityStorageVersion();

        if (targetVersion.isPresent() && sourceVersion == targetVersion.getAsLong()) {
            return ImmutableMap.of(identifier, entityData);
        }

        if (!entityMigrations.containsKey(entityType)) {
            throw new IllegalStateException(String.format("Cannot load entity '%s' with type '%s' and storageVersion '%d', no migrations found.", entityData.getId(), entityType, entityData.getEntityStorageVersion()));
        }

        Map<Long, EntityMigration<EntityData, EntityData>> migrations = entityMigrations.get(entityType);
        EntityData data = entityData;
        //final long maxSteps = targetVersion - sourceVersion;
        //long currentSteps = 0;

        /*sourceVersion < targetVersion && currentSteps < maxSteps*/
        while (targetVersion.isPresent() ? sourceVersion < targetVersion.getAsLong() : migrations.containsKey(sourceVersion)) {
            if (!migrations.containsKey(sourceVersion)) {
                throw new IllegalStateException(String.format("No migration found for entity schema: %s v%d.", entityType, sourceVersion));
            }

            data = migrations.get(sourceVersion)
                             .migrate(data);
            sourceVersion = data.getEntityStorageVersion();
            //currentSteps++;
        }

        Map<Identifier, EntityData> additionalEntities = migrations.get(entityData.getEntityStorageVersion())
                                                                   .getAdditionalEntities(identifier, entityData);

        return new ImmutableMap.Builder<Identifier, EntityData>().putAll(additionalEntities)
                                                                 .put(identifier, data)
                                                                 .build();
    }

    @Override
    public String getDataTypeName(Class<? extends EntityData> entityDataClass) {
        return entityDataTypes.get(entityDataClass);
    }

    @Override
    public CompletableFuture<PersistentEntity> createInstance(String entityType, String id, EntityData entityData) {

        LOGGER.debug("CREATING ENTITY {} {} {}", entityType, id/*, entityData*/);

        String instanceId = entityType + "/" + id;
        String specificEntityType = getSpecificEntityType(entityType, entityData.getEntitySubType());
        String instanceClassName = entityClasses.get(specificEntityType);

        ConfigurationBuilder instanceBuilder = declarationBuilderService.newInstance(instanceClassName)
                                                                        .name(instanceId)
                                                                        .configure()
                                                                        .property("data", entityData);

        if (entityHydrators.containsKey(specificEntityType)) {
            try {
                entityHydrators.get(specificEntityType)
                               .getInstanceConfiguration(entityData)
                               .forEach(instanceBuilder::property);
            } catch (Throwable e) {
                LOGGER.error("Entity of type '{}' with id '{}' could not be hydrated: {}", specificEntityType, id, e.getMessage());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Cause:", e);
                }
                throw e;
            }

        } else if (entityHydrators.containsKey(entityType)) {
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

        CompletableFuture<PersistentEntity> registration = new CompletableFuture<>();
        this.instanceRegistration.put(instanceId, registration);
        // wait max 5 secs, then proceed
        ScheduledFuture<Boolean> scheduledFuture = executorService.schedule(() -> registration.complete(null), 5, TimeUnit.SECONDS);

        DeclarationHandle handle = instanceBuilder.build();
        handle.publish();
        this.instanceHandles.put(instanceId, handle);

        return registration.whenComplete((entity, throwable) -> scheduledFuture.cancel(true));
    }

    @Override
    public CompletableFuture<PersistentEntity> updateInstance(String entityType, String id, EntityData entityData) {
        LOGGER.debug("UPDATING ENTITY {} {} {}", entityType, id/*, entityData*/);

        String instanceId = entityType + "/" + id;

        deleteInstance(entityType, id);
        return createInstance(entityType, id, entityData);

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

    private String getSpecificEntityType(String entityType, Optional<String> entitySubType) {
        return entitySubType.isPresent() ? String.format("%s/%s", entityType, entitySubType.get().toLowerCase()) : entityType;
    }
}
