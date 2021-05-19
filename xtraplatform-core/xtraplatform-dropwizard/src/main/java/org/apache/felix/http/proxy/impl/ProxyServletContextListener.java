/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.felix.http.proxy.impl;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.apache.felix.http.proxy.AbstractProxyListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

/**
 * The ProxyServletContextListener is a servlet context listener which will setup all required
 * listeners for the http service implementation.
 *
 * @since 3.0.0
 */
@WebListener
public class ProxyServletContextListener implements ServletContextListener {

  private volatile ServletContext servletContext;

  private volatile EventDispatcherTracker eventDispatcherTracker;

  // ---------- ServletContextListener

  @Override
  public void contextInitialized(final ServletContextEvent sce) {
    this.servletContext = sce.getServletContext();

    // add all required listeners

    this.servletContext.addListener(
        new AbstractProxyListener() {

          @Override
          protected void stopTracking() {
            ProxyServletContextListener.this.stopTracking();
          }

          @Override
          protected void startTracking(final Object bundleContextAttr) {
            ProxyServletContextListener.this.startTracking(bundleContextAttr);
          }

          @Override
          protected EventDispatcherTracker getEventDispatcherTracker() {
            return eventDispatcherTracker;
          }
        });
  }

  private void stopTracking() {
    if (eventDispatcherTracker != null) {
      eventDispatcherTracker.close();
      eventDispatcherTracker = null;
    }
  }

  protected void startTracking(final Object bundleContextAttr) {
    if (bundleContextAttr instanceof BundleContext) {
      try {
        final BundleContext bundleContext = (BundleContext) bundleContextAttr;
        eventDispatcherTracker = new EventDispatcherTracker(bundleContext);
        eventDispatcherTracker.open();
      } catch (final InvalidSyntaxException e) {
        // not expected for our simple filter
      }
    }
  }

  @Override
  public void contextDestroyed(final ServletContextEvent sce) {
    this.stopTracking();
    this.servletContext = null;
  }
}
