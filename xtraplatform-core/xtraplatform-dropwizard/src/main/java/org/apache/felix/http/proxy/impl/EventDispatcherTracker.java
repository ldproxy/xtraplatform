/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.felix.http.proxy.impl;

import java.util.EventListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

/** @since 3.0.0 */
public final class EventDispatcherTracker extends BridgeServiceTracker<EventListener> {
  private HttpSessionListener sessionListener;

  private HttpSessionIdListener sessionIdListener;

  private HttpSessionAttributeListener sessionAttributeListener;

  public EventDispatcherTracker(final BundleContext context) throws InvalidSyntaxException {
    super(context, EventListener.class);
  }

  @Override
  protected void setService(final EventListener service) {
    if (service instanceof HttpSessionListener) {
      this.sessionListener = (HttpSessionListener) service;
    }
    if (service instanceof HttpSessionIdListener) {
      this.sessionIdListener = (HttpSessionIdListener) service;
    }
    if (service instanceof HttpSessionAttributeListener) {
      this.sessionAttributeListener = (HttpSessionAttributeListener) service;
    }
  }

  public HttpSessionListener getHttpSessionListener() {
    return this.sessionListener;
  }

  public HttpSessionIdListener getHttpSessionIdListener() {
    return this.sessionIdListener;
  }

  public HttpSessionAttributeListener getHttpSessionAttributeListener() {
    return this.sessionAttributeListener;
  }

  @Override
  protected void unsetService() {
    sessionListener = null;
    sessionIdListener = null;
    sessionAttributeListener = null;
  }
}
