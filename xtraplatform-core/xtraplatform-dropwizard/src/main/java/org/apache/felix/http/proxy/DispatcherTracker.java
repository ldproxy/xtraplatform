/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.felix.http.proxy;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/** @deprecated */
@Deprecated
public final class DispatcherTracker extends ServiceTracker {
  static final String DEFAULT_FILTER = "(http.felix.dispatcher=*)";

  private final ServletConfig config;
  private HttpServlet dispatcher;

  public DispatcherTracker(BundleContext context, String filter, ServletConfig config)
      throws Exception {
    super(context, createFilter(context, filter), null);
    this.config = config;
  }

  public HttpServlet getDispatcher() {
    return this.dispatcher;
  }

  @Override
  public Object addingService(ServiceReference ref) {
    Object service = super.addingService(ref);
    if (service instanceof HttpServlet) {
      setDispatcher((HttpServlet) service);
    }

    return service;
  }

  @Override
  public void removedService(ServiceReference ref, Object service) {
    if (service instanceof HttpServlet) {
      setDispatcher(null);
    }

    super.removedService(ref, service);
  }

  private void log(String message, Throwable cause) {
    this.config.getServletContext().log(message, cause);
  }

  private void setDispatcher(HttpServlet dispatcher) {
    destroyDispatcher();
    this.dispatcher = dispatcher;
    initDispatcher();
  }

  private void destroyDispatcher() {
    if (this.dispatcher == null) {
      return;
    }

    this.dispatcher.destroy();
    this.dispatcher = null;
  }

  private void initDispatcher() {
    if (this.dispatcher == null) {
      return;
    }

    try {
      this.dispatcher.init(this.config);
    } catch (Exception e) {
      log("Failed to initialize dispatcher", e);
    }
  }

  private static Filter createFilter(BundleContext context, String filter) throws Exception {
    StringBuffer str = new StringBuffer();
    str.append("(&(").append(Constants.OBJECTCLASS).append("=");
    str.append(HttpServlet.class.getName()).append(")");
    str.append(filter != null ? filter : DEFAULT_FILTER).append(")");
    return context.createFilter(str.toString());
  }
}
