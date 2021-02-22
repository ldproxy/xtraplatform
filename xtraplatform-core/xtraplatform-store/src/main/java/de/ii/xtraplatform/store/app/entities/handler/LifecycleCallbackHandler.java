/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app.entities.handler;

import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;
import org.apache.felix.ipojo.util.Callback;

class LifecycleCallbackHandler extends PrimitiveHandler {

  /** The list of the callback of the component. */
  private LifecycleCallback[] m_callbacks = new LifecycleCallback[0];

  /** State of the instance manager (unresolved at the beginning). */
  private int m_state = InstanceManager.INVALID;
  /** Does a POJO object be created at starting. */
  private boolean m_immediate = false;

  /**
   * Add the given callback to the callback list.
   *
   * @param callback : the element to add
   */
  private void addCallback(LifecycleCallback callback) {
    for (int i = 0; (m_callbacks != null) && (i < m_callbacks.length); i++) {
      if (m_callbacks[i] == callback) {
        return;
      }
    }

    if (m_callbacks != null
        && m_callbacks.length > 0) { // TODO check here if we can improve the test
      LifecycleCallback[] newHk = new LifecycleCallback[m_callbacks.length + 1];
      System.arraycopy(m_callbacks, 0, newHk, 0, m_callbacks.length);
      newHk[m_callbacks.length] = callback;
      m_callbacks = newHk;
    } else {
      m_callbacks = new LifecycleCallback[] {callback};
    }
  }

  /**
   * Configure the handler.
   *
   * @param metadata : the component type metadata
   * @param configuration : the instance configuration
   * @throws ConfigurationException : one callback metadata is not correct (either the transition or
   *     the method are not correct).
   * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.metadata.Element,
   *     java.util.Dictionary)
   */
  public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {
    m_callbacks = new LifecycleCallback[0];

    String imm = metadata.getAttribute("immediate");
    m_immediate = imm != null && imm.equalsIgnoreCase("true");

    PojoMetadata meta = getFactory().getPojoMetadata();

    Element[] hooksMetadata = metadata.getElements("callback");
    for (int i = 0; hooksMetadata != null && i < hooksMetadata.length; i++) {
      String method = hooksMetadata[i].getAttribute("method");
      if (method == null) {
        throw new ConfigurationException(
            "Lifecycle callback : A callback needs to contain a method attribute");
      }

      MethodMetadata met = meta.getMethod(method, new String[0]);

      int transition = -1;
      String trans = hooksMetadata[i].getAttribute("transition");
      if (trans == null) {
        throw new ConfigurationException(
            "Lifecycle callback : the transition attribute is missing");
      } else {
        if (trans.equalsIgnoreCase("validate")) {
          transition = LifecycleCallback.VALIDATE;
        } else if (trans.equalsIgnoreCase("invalidate")) {
          transition = LifecycleCallback.INVALIDATE;
        } else {
          throw new ConfigurationException(
              "Lifecycle callback : Unknown or malformed transition : " + trans);
        }
      }

      LifecycleCallback callback = null;
      if (met == null) {
        callback = new LifecycleCallback(this, transition, method);
      } else {
        callback = new LifecycleCallback(this, transition, met);
      }
      addCallback(callback);
    }
  }

  /**
   * Start the handler.
   *
   * @see org.apache.felix.ipojo.Handler#start()
   */
  public void start() {
    // Do nothing during the start
  }

  /**
   * Stop the handler.
   *
   * @see org.apache.felix.ipojo.Handler#stop()
   */
  public void stop() {
    m_state = InstanceManager.INVALID;
  }

  /**
   * When the state change call the associated callback.
   *
   * @param state : the new instance state.
   * @see org.apache.felix.ipojo.Handler#stateChanged(int)
   */
  public void stateChanged(int state) {
    int transition = -1;
    if (m_state == ComponentInstance.INVALID && state == ComponentInstance.VALID) {
      transition = LifecycleCallback.VALIDATE;
    }
    if (m_state == ComponentInstance.VALID && state == ComponentInstance.INVALID) {
      transition = LifecycleCallback.INVALIDATE;
    }

    // Manage immediate component
    if (m_immediate
        && transition == LifecycleCallback.VALIDATE
        && getInstanceManager().getPojoObjects() == null) {
      getInstanceManager().getPojoObject();
    }

    for (int i = 0; i < m_callbacks.length; i++) {
      if (m_callbacks[i].getTransition() == transition) {
        try {
          m_callbacks[i].call();
        } catch (NoSuchMethodException e) {
          error(
              "["
                  + getInstanceManager().getInstanceName()
                  + "] The callback method "
                  + m_callbacks[i].getMethod()
                  + " is not found");
          throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
          error(
              "["
                  + getInstanceManager().getInstanceName()
                  + "] The callback method "
                  + m_callbacks[i].getMethod()
                  + " is not accessible");
          throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
          error(
              "["
                  + getInstanceManager().getInstanceName()
                  + "] The callback method "
                  + m_callbacks[i].getMethod()
                  + " has thrown an exception : "
                  + e.getTargetException().getMessage(),
              e.getTargetException());
          throw new IllegalStateException(e.getTargetException());
        }
      }
    }
    // Update to internal state
    m_state = state;
  }

  static class LifecycleCallback {

    /** Invalid to Valid transition. */
    protected static final int VALIDATE = 1;

    /** Valid to Invalid transition. */
    protected static final int INVALIDATE = 0;

    /** Transition on which calling the callback. */
    private int m_transition;

    /** Callback object. */
    private Callback m_callback;

    /**
     * LifecycleCallback constructor.
     *
     * @param handler : the callback handler calling the callback
     * @param transition : transition on which calling the callback
     * @param method : method metadata to invoke
     */
    public LifecycleCallback(
        LifecycleCallbackHandler handler, int transition, MethodMetadata method) {
      m_transition = transition;
      m_callback = new Callback(method, handler.getInstanceManager());
    }

    /**
     * LifecycleCallback constructor.
     *
     * @param handler : the callback handler calling the callback
     * @param transition : transition on which calling the callback
     * @param method : method name to invoke
     */
    public LifecycleCallback(LifecycleCallbackHandler handler, int transition, String method) {
      m_transition = transition;
      m_callback = new Callback(method, new String[0], false, handler.getInstanceManager());
    }

    /**
     * Call the callback method when the transition from inital tostate is detected.
     *
     * @throws NoSuchMethodException : Method is not found in the class
     * @throws InvocationTargetException : The method is not static
     * @throws IllegalAccessException : The method can not be invoked
     */
    protected void call()
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
      m_callback.call();
    }

    protected int getTransition() {
      return m_transition;
    }

    /**
     * Get the method name of the callback.
     *
     * @return the method name
     */
    protected String getMethod() {
      return m_callback.getMethod();
    }
  }
}
