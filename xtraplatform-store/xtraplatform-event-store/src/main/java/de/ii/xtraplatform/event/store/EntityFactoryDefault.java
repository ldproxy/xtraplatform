/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.event.store;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.EntityRegistry;
import de.ii.xtraplatform.entities.domain.PersistentEntity;
import de.ii.xtraplatform.entities.domain.handler.Entity;
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
import java.util.List;
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
                filter = "(&(objectClass=org.apache.felix.ipojo.Factory)(component.providedServiceSpecifications=de.ii.xtraplatform.entities.domain.PersistentEntity))",
                onArrival = "onFactoryArrival",
                onDeparture = "onFactoryDeparture"),
        @Wbp(
                filter = "(objectClass=de.ii.xtraplatform.event.store.EntityHydrator)",
                onArrival = "onHydratorArrival",
                onDeparture = "onHydratorDeparture"),
        @Wbp(
                filter = "(objectClass=de.ii.xtraplatform.event.store.EntityMigration)",
                onArrival = "onMigrationArrival",
                onDeparture = "onMigrationDeparture"),
        @Wbp(
                filter = "(objectClass=de.ii.xtraplatform.event.store.EntityDataDefaults)",
                onArrival = "onDefaultsArrival",
                onDeparture = "onDefaultsDeparture")
})
//TODO: use generic registry implementation
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
    private final Map<String, EntityDataDefaults<EntityData>> entityDataDefaults;
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
        this.entityDataDefaults = new ConcurrentHashMap<>();
        this.executorService = new ScheduledThreadPoolExecutor(1);

        entityRegistry.addEntityListener((instanceId, entity) -> {
            if (instanceRegistration.containsKey(instanceId)) {
                instanceRegistration.get(instanceId)
                                    .complete(entity);
            }
        });
    }

    private synchronized void onFactoryArrival(ServiceReference<ComponentFactory> ref) {
        Optional<String> entityClassName = getComponentClass(ref);
        Optional<String> entityType = getComponentProperty(ref, Entity.TYPE_KEY);
        Optional<String> entitySubType = getComponentProperty(ref, Entity.SUB_TYPE_KEY);
        Optional<String> entityDataClassName = getComponentProperty(ref, Entity.DATA_CLASS_KEY);
        Optional<String> entityDataSubClassName = getComponentProperty(ref, Entity.DATA_SUB_CLASS_KEY).filter(subClass -> !Objects.equals(subClass, "java.lang.Object"));

        if (entityClassName.isPresent() && entityDataClassName.isPresent() && entityType.isPresent()) {
            String commonEntityType = entityType.get();
            String specificEntityType = getSpecificEntityType(commonEntityType, entitySubType);
            ComponentFactory factory = context.getService(ref);

            try {
                registerEntityDataClass(factory, commonEntityType, entityDataClassName.get());
            } catch (ClassNotFoundException e) {
                LOGGER.error("Could not find class for entity data type {}.", entityDataClassName);
            }

            if (entityDataSubClassName.isPresent()) {
            try {
                    registerEntityDataClass(factory, specificEntityType, entityDataSubClassName.get());
                } catch (ClassNotFoundException e) {
                    LOGGER.error("Could not find class for entity data type {}.", entityDataClassName);
                }
            }

            //this.componentFactories.put(type, factory);
            this.entityClasses.put(specificEntityType, entityClassName.get());

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Registered entity type: {}", specificEntityType);
                              }
                }


                          }

    private Optional<String> getComponentClass(ServiceReference<ComponentFactory> ref) {
        return Optional.ofNullable((String) ref.getProperty("component.class"));
    }

    private Optional<String> getComponentProperty(ServiceReference<ComponentFactory> ref, String propertyKey) {
        return Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties"))
                     .filter(pd -> pd.getName()
                                     .equals(propertyKey))
                     .map(PropertyDescription::getValue)
                     .findFirst();
            }

    private Class<EntityDataBuilder<EntityData>> getDataBuilderClass(Class<?> dataClass) {
        JsonDeserialize annotation = dataClass.getAnnotation(JsonDeserialize.class);
        return (Class<EntityDataBuilder<EntityData>>) annotation.builder();
        }

    private void registerEntityDataClass(ComponentFactory factory, String entityType, String entityDataType) throws ClassNotFoundException {
        Class<?> entityDataClass = factory.loadClass(entityDataType);
        Class<EntityDataBuilder<EntityData>> builder = getDataBuilderClass(entityDataClass);

        this.entityDataBuilders.put(entityType, builder);
        this.entityDataTypes.put(entityDataClass, entityType);
    }

    //TODO
    private synchronized void onFactoryDeparture(ServiceReference<Factory> ref) {
        Optional<String> entityType = Optional.ofNullable((String) ref.getProperty("component.class"));
        Optional<String> entitySubType = Optional.ofNullable((String) ref.getProperty(Entity.SUB_TYPE_KEY));
        Optional<String> entityDataType = Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties"))
                                                .filter(pd -> pd.getName()
                                                                .equals(Entity.DATA_KEY))
                                                .map(PropertyDescription::getType)
                                                .findFirst();

        if (entityType.isPresent() && entityDataType.isPresent()) {
            String specificEntityType = getSpecificEntityType(entityType.get(), entitySubType);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Deregistered entity type: {}", specificEntityType);
            }
            //this.componentFactories.remove(entityDataType.get());
            this.entityClasses.remove(entityDataType.get());

        }
    }

    private synchronized void onHydratorArrival(ServiceReference<EntityHydrator<EntityData>> ref) {
        Optional<String> entityType = Optional.ofNullable((String) ref.getProperty(Entity.TYPE_KEY));
        Optional<String> entitySubType = Optional.ofNullable((String) ref.getProperty(Entity.SUB_TYPE_KEY));

        if (entityType.isPresent()) {
            String specificEntityType = getSpecificEntityType(entityType.get(), entitySubType);
            EntityHydrator<EntityData> entityHydrator = context.getService(ref);
            this.entityHydrators.put(specificEntityType, entityHydrator);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Registered entity data hydrator: {}", specificEntityType);
            }
        }
    }

    private synchronized void onHydratorDeparture(ServiceReference<EntityHydrator<EntityData>> ref) {
        try {
            Optional<String> entityType = Optional.ofNullable((String) ref.getProperty(Entity.TYPE_KEY));
            Optional<String> entitySubType = Optional.ofNullable((String) ref.getProperty(Entity.SUB_TYPE_KEY));

            if (entityType.isPresent()) {
                String specificEntityType = getSpecificEntityType(entityType.get(), entitySubType);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Deregistered entity data hydrator: {}", specificEntityType);
                }
                this.entityHydrators.remove(specificEntityType);
            }
        } catch (Throwable w) {
            //ignore
        }
    }

    private synchronized void onMigrationArrival(ServiceReference<EntityMigration<EntityData, EntityData>> ref) {
        Optional<String> entityType = Optional.ofNullable((String) ref.getProperty(Entity.TYPE_KEY));
        Optional<String> entitySubType = Optional.ofNullable((String) ref.getProperty(Entity.SUB_TYPE_KEY));

        if (entityType.isPresent()) {
            String specificEntityType = getSpecificEntityType(entityType.get(), entitySubType);
            EntityMigration<EntityData, EntityData> entityMigration = context.getService(ref);
            entityMigrations.putIfAbsent(specificEntityType, new ConcurrentHashMap<>());
            this.entityMigrations.get(specificEntityType)
                                 .put(entityMigration.getSourceVersion(), entityMigration);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Registered entity schema migration: {} v{} -> v{}", specificEntityType, entityMigration.getSourceVersion(), entityMigration.getTargetVersion());
            }
        }
    }

    private synchronized void onMigrationDeparture(ServiceReference<EntityMigration<EntityData, EntityData>> ref) {
        try {
            Optional<String> entityType = Optional.ofNullable((String) ref.getProperty(Entity.TYPE_KEY));
            Optional<String> entitySubType = Optional.ofNullable((String) ref.getProperty(Entity.SUB_TYPE_KEY));

            if (entityType.isPresent()) {
                String specificEntityType = getSpecificEntityType(entityType.get(), entitySubType);
                EntityMigration<EntityData, EntityData> entityMigration = context.getService(ref);
                this.entityMigrations.get(specificEntityType)
                                     .remove(entityMigration.getSourceVersion());

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Deregistered entity schema migration: {} v{} -> v{}", specificEntityType, entityMigration.getSourceVersion(), entityMigration.getTargetVersion());
                }
            }
        } catch (Throwable w) {
            //ignore
        }
    }

    private synchronized void onDefaultsArrival(ServiceReference<EntityDataDefaults<EntityData>> ref) {
        Optional<String> entityType = Optional.ofNullable((String) ref.getProperty(Entity.TYPE_KEY));
        Optional<String> entitySubType = Optional.ofNullable((String) ref.getProperty(Entity.SUB_TYPE_KEY));

        if (entityType.isPresent()) {
            String specificEntityType = getSpecificEntityType(entityType.get(), entitySubType);
            EntityDataDefaults<EntityData> defaults = context.getService(ref);
            this.entityDataDefaults.put(specificEntityType, defaults);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Registered entity data defaults: {}", specificEntityType);
            }
        }
    }

    private synchronized void onDefaultsDeparture(ServiceReference<EntityDataDefaults<EntityData>> ref) {
        try {
            Optional<String> entityType = Optional.ofNullable((String) ref.getProperty(Entity.TYPE_KEY));
            Optional<String> entitySubType = Optional.ofNullable((String) ref.getProperty(Entity.SUB_TYPE_KEY));

            if (entityType.isPresent()) {
                String specificEntityType = getSpecificEntityType(entityType.get(), entitySubType);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Deregistered entity data defaults: {}", specificEntityType);
                }
                this.entityDataDefaults.remove(specificEntityType);
            }
        } catch (Throwable w) {
            //ignore
        }
    }

    @Override
    public EntityDataBuilder<EntityData> getDataBuilder(String entityType, Optional<String> entitySubType) {
        String specificEntityType = getSpecificEntityType(entityType, entitySubType);

        if (!entityDataBuilders.containsKey(specificEntityType)) {
            throw new IllegalStateException("no builder found for entity type " + entityType);
        }

        try {
            if (entityDataDefaults.containsKey(specificEntityType)) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("USING DEFAULTS {}", specificEntityType);
                }
                return entityDataDefaults.get(specificEntityType).getBuilderWithDefaults();
            }

            return entityDataBuilders.get(specificEntityType)
                                     .newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error("Could not instantiate builder for entity type {}.", specificEntityType);
        }

        return null;
    }

    @Override
    public Optional<EntityDataDefaults.KeyPathAlias> getKeyPathAlias(String keyPath) {
        return entityDataDefaults.values()
                                 .stream()
                                 .flatMap(defaults -> defaults.getAliases().entrySet().stream())
                                 .filter(entry -> Objects.equals(keyPath, entry.getKey()))
                                 .map(Map.Entry::getValue)
                                 .findFirst();
    }

    @Override
    public List<List<String>> getSubTypes(String entityType, List<String> entitySubType) {
        String specificEntityType = getSpecificEntityType(entityType, getTypeAsString(entitySubType));

        return entityDataBuilders.keySet()
                                 .stream()
                                 .filter(key -> key.startsWith(specificEntityType) && !Objects.equals(key, specificEntityType))
                                 .map(this::getEntitySubType)
                                 .filter(Optional::isPresent)
                                 .map(Optional::get)
                                 .map(subType -> Splitter.on('/').splitToList(subType))
                                 .collect(ImmutableList.toImmutableList());
    }

    @Override
    public EntityDataBuilder<EntityData> getDataBuilders(String entityType, long entitySchemaVersion, Optional<String> entitySubType) {
        String specificEntityType = getSpecificEntityType(entityType, entitySubType);

        if (!entityMigrations.containsKey(specificEntityType) && !entityMigrations.containsKey(entityType)) {
            throw new IllegalStateException(String.format("Cannot migrate entity with type '%s' and storageVersion '%d', no migrations found.", specificEntityType, entitySchemaVersion));
        }

        Map<Long, EntityMigration<EntityData, EntityData>> migrations = entityMigrations.containsKey(specificEntityType) ? entityMigrations.get(specificEntityType) : entityMigrations.get(entityType);

        try {
            return migrations.get(entitySchemaVersion)
                                   .getDataBuilder();
        } catch (Throwable e) {
            throw new IllegalStateException("no builder found for entity type " + entityType);
        }
    }

    @Override
    public List<String> getTypeAsList(String entitySubtype) {
        return Splitter.on('/').splitToList(entitySubtype.toLowerCase());
    }

    @Override
    public Optional<String> getTypeAsString(List<String> entitySubtype) {
        if (entitySubtype.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(Joiner.on('/')
                                 .join(entitySubtype));
    }

    @Override
    public Map<Identifier, EntityData> migrateSchema(Identifier identifier,
                                                     String entityType, EntityData entityData,
                                                     Optional<String> entitySubType, OptionalLong targetVersion) {
        long sourceVersion = entityData.getEntityStorageVersion();

        if (targetVersion.isPresent() && sourceVersion == targetVersion.getAsLong()) {
            return ImmutableMap.of(identifier, entityData);
        }

        Optional<String> subType = entitySubType.isPresent() ? entitySubType : entityData.getEntitySubType();
        String specificEntityType = getSpecificEntityType(entityType, subType);

        if (!entityMigrations.containsKey(specificEntityType) && !entityMigrations.containsKey(entityType)) {
            throw new IllegalStateException(String.format("Cannot load entity '%s' with type '%s' and storageVersion '%d', no migrations found.", entityData.getId(), specificEntityType, entityData.getEntityStorageVersion()));
        }

        Map<Long, EntityMigration<EntityData, EntityData>> migrations = entityMigrations.containsKey(specificEntityType) ? entityMigrations.get(specificEntityType) : entityMigrations.get(entityType);
        EntityData data = entityData;
        //final long maxSteps = targetVersion - sourceVersion;
        //long currentSteps = 0;

        /*sourceVersion < targetVersion && currentSteps < maxSteps*/
        while (targetVersion.isPresent() ? sourceVersion < targetVersion.getAsLong() : migrations.containsKey(sourceVersion)) {
            if (!migrations.containsKey(sourceVersion)) {
                throw new IllegalStateException(String.format("No migration found for entity schema: %s v%d.", specificEntityType, sourceVersion));
            }

            EntityMigration<EntityData, EntityData> migration = migrations.get(sourceVersion);
            data = migration.migrate(data);
            sourceVersion = data.getEntityStorageVersion();
            //currentSteps++;
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Migrated schema for entity '{}' with type '{}': v{} -> v{}", entityData.getId(), specificEntityType, migration.getSourceVersion(), migration.getTargetVersion());
            }
        }

        Map<Identifier, EntityData> additionalEntities = migrations.get(entityData.getEntityStorageVersion())
                                                                   .getAdditionalEntities(identifier, entityData);

        return new ImmutableMap.Builder<Identifier, EntityData>().putAll(additionalEntities)
                                                                 .put(identifier, data)
                                                                 .build();
    }

    @Override
    public EntityData hydrateData(Identifier identifier, String entityType, EntityData entityData) {

        String specificEntityType = getSpecificEntityType(entityType, entityData.getEntitySubType());
        String id = identifier.id();

        if (entityHydrators.containsKey(specificEntityType)) {
            try {
                return entityHydrators.get(specificEntityType)
                                      .hydrateData(entityData);

            } catch (Throwable e) {
                LOGGER.error("Entity of type '{}' with id '{}' could not be hydrated: {}", specificEntityType, id, e.getMessage());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Cause:", e);
                }
                //throw e;
            }

        } else if (entityHydrators.containsKey(entityType)) {
            try {
                return entityHydrators.get(entityType)
                                      .hydrateData(entityData);

            } catch (Throwable e) {
                LOGGER.error("Entity of type '{}' with id '{}' could not be hydrated: {}", entityType, id, e.getMessage());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Cause:", e);
                }
                //throw e;
            }

        }

        return entityData;
    }

    @Override
    public String getDataTypeName(Class<? extends EntityData> entityDataClass) {
        return entityDataTypes.get(entityDataClass);
    }

    @Override
    public CompletableFuture<PersistentEntity> createInstance(String entityType, String id, EntityData entityData) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("CREATING ENTITY {} {} {}", entityType, id/*, entityData*/);
        }
        String instanceId = entityType + "/" + id;
        String specificEntityType = getSpecificEntityType(entityType, entityData.getEntitySubType());
        String instanceClassName = entityClasses.get(specificEntityType);

        ConfigurationBuilder instanceBuilder = declarationBuilderService.newInstance(instanceClassName)
                                                                        .name(instanceId)
                                                                        .configure()
                                                                        .property(Entity.DATA_KEY, entityData);

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
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("UPDATING ENTITY {} {} {}", entityType, id/*, entityData*/);
        }

        String instanceId = entityType + "/" + id;

        deleteInstance(entityType, id);
        return createInstance(entityType, id, entityData);

        /*if (instanceHandles.containsKey(instanceId)) {
            Dictionary<String, Object> configuration = new Hashtable<>();
            configuration.put("instance.name", instanceId);
            configuration.put(Entity.DATA_KEY, entityData);

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
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("DELETING ENTITY {} {}", entityType, id);
        }

        String instanceId = entityType + "/" + id;

        if (instanceHandles.containsKey(instanceId)) {
            instanceHandles.get(instanceId)
                           .retract();
            instanceHandles.remove(instanceId);
        }
    }

    private String getSpecificEntityType(String entityType, Optional<String> entitySubType) {
        return entitySubType.isPresent() ? String.format("%s/%s", entityType, entitySubType.get()
                                                                                           .toLowerCase()) : entityType;
    }

    private String getCommonEntityType(String specificEntityType) {
        return specificEntityType.contains("/") ? specificEntityType.substring(0, specificEntityType.indexOf("/")) : specificEntityType;
    }

    private Optional<String> getEntitySubType(String specificEntityType) {
        return specificEntityType.contains("/") ? Optional.of(specificEntityType.substring(specificEntityType.indexOf("/") + 1)) : Optional.empty();
    }
}
