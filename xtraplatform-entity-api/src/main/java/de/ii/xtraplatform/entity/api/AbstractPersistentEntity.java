/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entity.api;

import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
public abstract class AbstractPersistentEntity<T extends EntityData> implements PersistentEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPersistentEntity.class);

    @ServiceController(value=false) // is ignored here, but added by @Entity handler
    public boolean register;

    private T data;

    @Override
    public T getData() {
        return data;
    }

    @Property(name = "data") // is ignored here, but added by @Entity handler
    public void setData(T data) {
        LOGGER.debug("GOT data {}"/*, data*/);
        this.data = dataToImmutable(data);

        if (shouldRegister()) {
            LOGGER.debug("REGISTERED {}", data.getId());
            this.register = true;
            //this.__IM.onSet(this, "register", true);

        }
    }

    protected boolean shouldRegister() {
        return true;
    }

    protected abstract T dataToImmutable(T data);
}
