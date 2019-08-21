/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.event.store;

import com.google.common.collect.ObjectArrays;
import de.ii.xtraplatform.dropwizard.api.Jackson;
import de.ii.xtraplatform.entity.api.EntityData;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */

@Component(publicFactory = false)
@Provides
@Instantiate
public class EntityStore extends AbstractEntityDataStore<EntityData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityStore.class);
    private static final String EVENT_TYPE = "entities";

    private final EntityFactory entityFactory;

    protected EntityStore(@Requires EventStore eventStore, @Requires Jackson jackson,
                          @Requires EntityFactory entityFactory) {
        super(eventStore, EVENT_TYPE, jackson.getDefaultObjectMapper());

        this.entityFactory = entityFactory;
    }

    @Override
    protected EntityDataBuilder<EntityData> getBuilder(Identifier identifier) {
        return entityFactory.getDataBuilder(identifier.path()
                                                      .get(0));
    }

    @Override
    protected void onStart() {
        //TODO: getAllPaths
        identifiers().forEach(identifier -> {
            onCreate(identifier, get(identifier));
        });
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
        return new EntityStoreDecorator<EntityData,U>() {
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
}
