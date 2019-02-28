/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.cfgstore.api.entity;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Controller;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.PostRegistration;
import org.apache.felix.ipojo.annotations.PostUnregistration;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
@Component(factoryMethod = "provider")
//@Component
//@Provides
//@Entity
//@FromStore(name = "de.ii.ldproxy.service.LdProxyServiceStore")
public class EntityTestImpl extends AbstractEntity<EntityConfigurationTestImpl> {

    public static EntityTestImpl provider() {
        return new EntityTestImpl();
    }

    public EntityTestImpl() {
        super();
    }

    public EntityTestImpl(String id, EntityConfigurationTestImpl value) {
        super(value);
        this.id = id;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityTestImpl.class);

    private static int REVISION = 0;

    private String id;

    @ServiceController(value=false)
    private boolean online;

    @Controller
    private boolean valid;

    @Property(name = "value")
    @Override
    void setValue(EntityConfigurationTestImpl value) {
        super.setValue(value);
    }

    @Override
    public String getResourceId() {
        return id;
    }

    @Property(name = "id")
    @Override
    public void setResourceId(String id) {
        LOGGER.debug("GOT id {}", id);
        this.id = id;
        ++REVISION;
    }

    //@Property(name = "online")
    public void setOnline(boolean online) {
        LOGGER.debug("GOT online {}", online);
        this.online = online;
    }

    @Validate
    void onStart() {
        LOGGER.debug("STARTED {} rev{} {}", id, REVISION, online);
    }

    @PostRegistration
    void onPublish(ServiceReference ref) {
        LOGGER.debug("PUBLISHED {} rev{} {}", id, REVISION, online);
    }

    @Invalidate
    void onStop() {
        LOGGER.debug("STOPPED {} rev{} {}", id, REVISION, online);
    }

    @PostUnregistration
    void onHide(ServiceReference ref) {
        LOGGER.debug("HIDDEN {} rev{} {}", id, REVISION, online);
    }
}
