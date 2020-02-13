/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.event.store;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.collect.ObjectArrays;
import de.ii.xtraplatform.dropwizard.api.Jackson;
import de.ii.xtraplatform.entity.api.EntityData;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

/**
 * @author zahnen
 */

@Component(publicFactory = false)
@Provides
@Instantiate
public class EntityStore extends AbstractEntityDataStore<EntityData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityStore.class);
    private static final String EVENT_TYPE = "entities";

    private final EventStore eventStore;
    private final EntityFactory entityFactory;
    private final Map<Identifier, EntityData> additionalEvents;

    protected EntityStore(@Requires EventStore eventStore, @Requires Jackson jackson,
                          @Requires EntityFactory entityFactory) {
        super(eventStore, EVENT_TYPE, jackson.getDefaultObjectMapper(), jackson.getNewObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
                                                                                                                    .disable(YAMLGenerator.Feature.USE_NATIVE_OBJECT_ID)
                                                                                                                    .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)));

        this.eventStore = eventStore;
        this.entityFactory = entityFactory;
        this.additionalEvents = new LinkedHashMap<>();
    }

    @Override
    protected EntityDataBuilder<EntityData> getBuilder(Identifier identifier) {
        return entityFactory.getDataBuilder(identifier.path()
                                                      .get(0));
    }

    @Override
    protected EntityDataBuilder<EntityData> getBuilder(Identifier identifier, long entitySchemaVersion) {
        return entityFactory.getDataBuilder(identifier.path()
                                                      .get(0), entitySchemaVersion);
    }

    @Override
    protected Map<Identifier, EntityData> migrate(Identifier identifier, EntityData entityData, OptionalLong targetVersion) {
        String entityType = identifier.path()
                                      .get(0);
        return entityFactory.migrateSchema(identifier, entityType, entityData, targetVersion);
    }

    @Override
    protected void addAdditionalEvent(Identifier identifier, EntityData entityData) {
        additionalEvents.put(identifier, entityData);
    }

    @Override
    protected CompletableFuture<Void> onStart() {
        playAdditionalEvents();

        //TODO: getAllPaths
        identifiers().stream()
                     //TODO: set priority per entity type (for now alphabetic works: codelists < providers < services)
                     .sorted(Comparator.comparing(identifier -> identifier.path()
                                                                          .get(0)))
                     .forEach(identifier -> {
            try {
                onCreate(identifier, get(identifier));
            } catch (Throwable e) {

            }
        });

        //TODO: return future from playAdditionalEvents
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected void onCreate(Identifier identifier, EntityData entityData) {
        entityFactory.createInstance(identifier.path()
                                               .get(0), identifier.id(), entityData);
        LOGGER.debug("Entity created: {}", identifier);
    }

    @Override
    protected void onUpdate(Identifier identifier, EntityData entityData) {
        entityFactory.updateInstance(identifier.path()
                                               .get(0), identifier.id(), entityData);
    }

    @Override
    protected void onDelete(Identifier identifier) {
        entityFactory.deleteInstance(identifier.path()
                                               .get(0), identifier.id());
    }

    @Override
    protected void onFailure(Identifier identifier, Throwable throwable) {

    }

    @Override
    public <U extends EntityData> EntityDataStore<U> forType(Class<U> type) {
        final String typeCollectionName = entityFactory.getDataTypeName(type);// type.getSimpleName() + "s";
        return new EntityStoreDecorator<EntityData, U>() {
            @Override
            public EntityDataStore<EntityData> getDecorated() {
                return EntityStore.this;
            }

            @Override
            public String[] transformPath(String... path) {
                return ObjectArrays.concat(typeCollectionName, path);
            }
        };
    }

    //TODO: test read-only
    private void playAdditionalEvents() {
        additionalEvents.forEach(((identifier, entityData) -> {
            if (eventStore.isReadOnly()) {
                onEmit(ImmutableMutationEvent.builder()
                                             .type(EVENT_TYPE)
                                             .identifier(identifier)
                                             .payload(serialize(entityData))
                                             .format(DEFAULT_FORMAT.name())
                                             .build());
            } else {
                //if (has(identifier)) {
                    dropWithoutTrigger(identifier).whenComplete((aBoolean, throwable) -> {
                        put(identifier, entityData);//.join();
                    });//TODO .join();
                //} else {
                //    put(identifier, entityData);//.join();
                //}
            }
        }));
    }
}
