/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.cfgstore.api.entity;

import de.ii.xsf.configstore.api.KeyValueStore;
import de.ii.xsf.configstore.api.rest.AbstractGenericResourceStore;
import de.ii.xsf.dropwizard.api.Jackson;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zahnen
 */
@Component
@Provides(properties = {@StaticServiceProperty(name = "cfgForEntity", value = "de.ii.xsf.cfgstore.api.entity.EntityTestImpl", type = "java.lang.String")})
//@Instantiate
public class EntityConfigurationStoreImpl extends AbstractGenericResourceStore<EntityTestImpl, EntityConfigurationStore> implements EntityConfigurationStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityConfigurationStoreImpl.class);

    public static final String STORE_ID = "test-entity-store";

    public EntityConfigurationStoreImpl(@Requires Jackson jackson, @Requires KeyValueStore rootConfigStore) {
        super(rootConfigStore, STORE_ID, jackson.getDefaultObjectMapper());
    }

    @Override
    protected EntityTestImpl createEmptyResource(String id, String... path) {
        return new EntityTestImpl();
    }

    @Override
    protected Class<?> getResourceClass(String id, String... path) {
        return null;
    }
}
