/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.felix.http.proxy;

import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;
import org.apache.felix.http.proxy.impl.EventDispatcherTracker;
import org.osgi.framework.BundleContext;

/**
 * The <code>ProxyListener</code> implements a Servlet API listener for HTTP Session related events.
 * These events are provided by the servlet container and forwarded to the event dispatcher.
 *
 * @since 3.1.0
 */
public abstract class AbstractProxyListener
    implements HttpSessionListener,
        HttpSessionIdListener,
        HttpSessionAttributeListener,
        ServletContextAttributeListener {

  private static final String ATTR_BUNDLE_CONTEXT = BundleContext.class.getName();

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

  // ServletContextAttributeListener

  @Override
  public void attributeAdded(final ServletContextAttributeEvent event) {
    if (event.getName().equals(ATTR_BUNDLE_CONTEXT)) {
      startTracking(event.getValue());
    }
  }

  @Override
  public void attributeRemoved(final ServletContextAttributeEvent event) {
    if (event.getName().equals(ATTR_BUNDLE_CONTEXT)) {
      stopTracking();
    }
  }

  @Override
  public void attributeReplaced(final ServletContextAttributeEvent event) {
    if (event.getName().equals(ATTR_BUNDLE_CONTEXT)) {
      stopTracking();
      startTracking(event.getServletContext().getAttribute(event.getName()));
    }
  }

  // ---------- internal

  protected abstract EventDispatcherTracker getEventDispatcherTracker();

  protected abstract void stopTracking();

  protected abstract void startTracking(Object bundleContext);

  private HttpSessionListener getSessionDispatcher() {
    final EventDispatcherTracker eventDispatcherTracker = getEventDispatcherTracker();
    if (eventDispatcherTracker != null) {
      return eventDispatcherTracker.getHttpSessionListener();
    }
    return null;
  }

  private HttpSessionIdListener getSessionIdDispatcher() {
    final EventDispatcherTracker eventDispatcherTracker = getEventDispatcherTracker();
    if (eventDispatcherTracker != null) {
      return eventDispatcherTracker.getHttpSessionIdListener();
    }
    return null;
  }

  private HttpSessionAttributeListener getAttributeDispatcher() {
    final EventDispatcherTracker eventDispatcherTracker = getEventDispatcherTracker();
    if (eventDispatcherTracker != null) {
      return eventDispatcherTracker.getHttpSessionAttributeListener();
    }
    return null;
  }
}
