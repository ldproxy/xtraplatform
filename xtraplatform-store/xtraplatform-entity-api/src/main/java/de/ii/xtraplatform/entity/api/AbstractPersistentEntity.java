/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entity.api;

import de.ii.xtraplatform.entity.api.handler.Entity;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
public abstract class AbstractPersistentEntity<T extends EntityData> implements PersistentEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPersistentEntity.class);

    @ServiceController(value = false) // is ignored here, but added by @Entity handler
    public boolean register;

    private T data;

    @Validate// is ignored here, but added by @EntityComponent stereotype
    public final void onValidate() {
        onStart();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("STARTED {} {} {}", getId(), shouldRegister(), register);
        }
    }

    protected void onStart() {
    }

    @Override
    public T getData() {
        return data;
    }

    @Property(name = Entity.DATA_KEY) // is ignored here, but added by @Entity handler
    public final void setData(T data) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("GOT data {}"/*, data*/);
        }
        this.data = data;

        if (shouldRegister()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("REGISTERED {}", data.getId());
            }
            this.register = true;

        } else {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("DEREGISTERED {}", data.getId());
            }
            this.register = false;
        }
    }

    protected boolean shouldRegister() {
        return false;
    }
}
