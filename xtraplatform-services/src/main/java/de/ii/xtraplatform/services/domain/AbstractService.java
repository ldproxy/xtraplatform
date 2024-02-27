/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.domain;

import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.entities.domain.AbstractPersistentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
public abstract class AbstractService<T extends ServiceData> extends AbstractPersistentEntity<T>
    implements Service {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractService.class);

  public AbstractService(T data, VolatileRegistry volatileRegistry) {
    super(data, volatileRegistry);
  }

  @Override
  protected final boolean shouldRegister() {
    return getData() != null && getData().getEnabled();
  }

  @Override
  protected void onStarted() {
    LOGGER.info("Service with id '{}' started successfully.", getId());
  }

  @Override
  protected void onReloaded() {
    LOGGER.info("Service with id '{}' reloaded successfully.", getId());
  }

  @Override
  protected void onStopped() {
    LOGGER.info("Service with id '{}' stopped.", getId());
  }

  @Override
  protected void onStartupFailure(Throwable throwable) {
    LogContext.error(LOGGER, throwable, "Service with id '{}' could not be started", getId());
  }
}
