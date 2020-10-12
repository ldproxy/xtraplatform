/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain.entities;

import de.ii.xtraplatform.runtime.domain.Logging;
import de.ii.xtraplatform.store.domain.entities.handler.Entity;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;


/** @author zahnen */
public abstract class AbstractPersistentEntity<T extends EntityData> implements PersistentEntity {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPersistentEntity.class);

  @ServiceController(value = false) // is ignored here, but added by @Entity handler
  public boolean register;

  private T data;

  @Validate // is ignored here, but added by @EntityComponent stereotype
  public final void onValidate() {
    try(MDC.MDCCloseable closeable = Logging.putCloseable(Logging.CONTEXT.SERVICE, getId())) {
      onStart();
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("STARTED {} {} {}", getId(), shouldRegister(), register);
      }
    }
  }

  @Invalidate // is ignored here, but added by @EntityComponent stereotype
  public final void onInvalidate() {
    try(MDC.MDCCloseable closeable = Logging.putCloseable(Logging.CONTEXT.SERVICE, getId())) {
      onStop();
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("STOPPED {} {} {}", getId(), shouldRegister(), register);
      }
    }
  }

  protected void onStart() {}

  protected void onStop() {}

  @Override
  public T getData() {
    return data;
  }

  @Property(name = Entity.DATA_KEY) // is ignored here, but added by @Entity handler
  public final void setData(T data) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("GOT data {}" /*, data*/);
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
