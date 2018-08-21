/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.cfgstore.api.entity;

import org.apache.felix.ipojo.annotations.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
public class AbstractEntity<T extends EntityConfiguration> implements Entity<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEntity.class);

    private T value;

    AbstractEntity() {

    }

    AbstractEntity(T value) {
        this.value = value;
    }

    @Property(name = "value")
    void setValue(T value) {
        LOGGER.debug("GOT value {}", value);
        this.value = value;
    }

    @Property(name = "data")
    public void setData(T data) {
        LOGGER.debug("GOT data {}", data);
        this.value = data;
    }

    @Override
    public T getData() {
        return value;
    }

    @Override
    public String getResourceId() {
        return value.getResourceId();
    }

    // TODO: remove in resource
    @Override
    public void setResourceId(String id) {

    }
}
