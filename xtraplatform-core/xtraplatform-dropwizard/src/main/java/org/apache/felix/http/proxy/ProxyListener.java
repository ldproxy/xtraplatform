/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.felix.http.proxy;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;
import org.apache.felix.http.proxy.impl.EventDispatcherTracker;
import org.apache.felix.http.proxy.impl.ProxyServletContextListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

/**
 * The <code>ProxyListener</code> implements a Servlet API listener for HTTP Session related events.
 * These events are provided by the servlet container and forwarded to the event dispatcher.
 *
 * @since 2.1.0
 * @deprecated Use the {@link ProxyServletContextListener} instead.
 */
@Deprecated
public class ProxyListener
    implements HttpSessionAttributeListener,
        HttpSessionListener,
        HttpSessionIdListener,
        ServletContextListener {

  private volatile ServletContext servletContext;

  private volatile EventDispatcherTracker eventDispatcherTracker;

  // ---------- ServletContextListener

  @Override
  public void contextInitialized(final ServletContextEvent sce) {
    this.servletContext = sce.getServletContext();
  }

  @Override
  public void contextDestroyed(final ServletContextEvent sce) {
    if (this.eventDispatcherTracker != null) {
      this.eventDispatcherTracker.close();
      this.eventDispatcherTracker = null;
    }
    this.servletContext = null;
  }

  // ---------- HttpSessionListener

  @Override
  public void sessionCreated(final HttpSessionEvent se) {
    final HttpSessionListener sessionDispatcher = getSessionDispatcher();
    if (sessionDispatcher != null) {
      sessionDispatcher.sessionCreated(se);
    }
  }

  @Override
  public void sessionDestroyed(final HttpSessionEvent se) {
    final HttpSessionListener sessionDispatcher = getSessionDispatcher();
    if (sessionDispatcher != null) {
      sessionDispatcher.sessionDestroyed(se);
    }
  }

  // ---------- HttpSessionIdListener

  @Override
  public void sessionIdChanged(final HttpSessionEvent event, final String oldSessionId) {
    final HttpSessionIdListener sessionIdDispatcher = getSessionIdDispatcher();
    if (sessionIdDispatcher != null) {
      sessionIdDispatcher.sessionIdChanged(event, oldSessionId);
    }
  }

  // ---------- HttpSessionAttributeListener

  @Override
  public void attributeAdded(final HttpSessionBindingEvent se) {
    final HttpSessionAttributeListener attributeDispatcher = getAttributeDispatcher();
    if (attributeDispatcher != null) {
      attributeDispatcher.attributeAdded(se);
    }
  }

  @Override
  public void attributeRemoved(final HttpSessionBindingEvent se) {
    final HttpSessionAttributeListener attributeDispatcher = getAttributeDispatcher();
    if (attributeDispatcher != null) {
      attributeDispatcher.attributeRemoved(se);
    }
  }

  @Override
  public void attributeReplaced(final HttpSessionBindingEvent se) {
    final HttpSessionAttributeListener attributeDispatcher = getAttributeDispatcher();
    if (attributeDispatcher != null) {
      attributeDispatcher.attributeReplaced(se);
    }
  }

  // ---------- internal

  private Object getDispatcher() {
    if (this.eventDispatcherTracker == null) {
      // the bundle context may or may not be already provided;
      // if not we cannot access the dispatcher yet
      Object bundleContextAttr = this.servletContext.getAttribute(BundleContext.class.getName());
      if (!(bundleContextAttr instanceof BundleContext)) {
        return null;
      }

      try {
        BundleContext bundleContext = (BundleContext) bundleContextAttr;
        this.eventDispatcherTracker = new EventDispatcherTracker(bundleContext);
        this.eventDispatcherTracker.open();
      } catch (InvalidSyntaxException e) {
        // not expected for our simple filter
      }
    }
    return this.eventDispatcherTracker.getService();
  }

  private HttpSessionListener getSessionDispatcher() {
    if (this.eventDispatcherTracker != null) {
      return this.eventDispatcherTracker.getHttpSessionListener();
    }
    return null;
  }

  private HttpSessionIdListener getSessionIdDispatcher() {
    if (this.eventDispatcherTracker != null) {
      return this.eventDispatcherTracker.getHttpSessionIdListener();
    }
    return null;
  }

  private HttpSessionAttributeListener getAttributeDispatcher() {
    if (this.eventDispatcherTracker != null) {
      return this.eventDispatcherTracker.getHttpSessionAttributeListener();
    }
    return null;
  }
}
