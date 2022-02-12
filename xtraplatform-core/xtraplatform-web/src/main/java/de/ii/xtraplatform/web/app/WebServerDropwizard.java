/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import de.ii.xtraplatform.web.domain.Dropwizard;
import de.ii.xtraplatform.base.domain.LogContext;
import io.dropwizard.jetty.NonblockingServletHolder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.ServletContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.MultiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author zahnen */
//TODO: no longer needed
public class WebServerDropwizard {

  private static final Logger LOGGER = LoggerFactory.getLogger(WebServerDropwizard.class);
  private static final String APP_ENDPOINT = "/*";
  static final String JERSEY_ENDPOINT = "/rest/*";

  private final Dropwizard dw;
  private final AdminEndpointServlet adminEndpoint;

  private boolean initialized;
  private Server server;
  private String url;

  private boolean started;
  private final Lock startStopLock;
  private final ScheduledExecutorService startStopThread;

  public WebServerDropwizard(
      Dropwizard dw,
      AdminEndpointServlet adminEndpoint) {

    this.dw = dw;
    this.adminEndpoint = adminEndpoint;
    this.url = "";

    this.startStopLock = new ReentrantLock();

    this.startStopThread =
        Executors.newSingleThreadScheduledExecutor(
            new ThreadFactory() {
              @Override
              public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "startup");
                t.setDaemon(true);
                return t;
              }
            });
  }

  private enum StartStopAction {
    START,
    STOP,
    RESTART
  }

  private class StartStop implements Runnable {

    private final StartStopAction action;

    public StartStop(StartStopAction action) {
      this.action = action;
    }

    @Override
    public void run() {
      startStopLock.lock();
      try {
        if (started && (action == StartStopAction.STOP || action == StartStopAction.RESTART)) {
          if (action == StartStopAction.STOP) {
            Thread.currentThread().setName("shutdown");
          }
          try {
            String u = getUrl();

            server.stop();
            server.join();

            started = false;

            LOGGER.info("Stopped web server at {}", u);

          } catch (MultiException ex) {
            for (Throwable e : ex.getThrowables()) {
              if (e != null) {
                LogContext.error(LOGGER, e, "Error stopping web server");
              }
            }
          } catch (Exception e) {
            LogContext.error(LOGGER, e, "Error stopping web server");
          }

          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            // ignore
          }
        }
        if (!started && (action == StartStopAction.START || action == StartStopAction.RESTART)) {
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("DW START {}", Thread.currentThread().getName());
          }
          cleanup();

          try {
            server.start();

            started = true;

            LOGGER.info("Started web server at {}", getUrl());

          } catch (Exception e) {
            LogContext.error(LOGGER, e, "Error stopping web server");
          }
        }
      } finally {
        startStopLock.unlock();
      }
    }
  }

  //@Validate
  protected void startBundle() {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("DW STARTBUNDLE");
    }

    start();
  }

  //@Invalidate
  protected void stopBundle() {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("DW STOPBUNDLE");
    }

    stop();

    // startStopThread.shutdownNow();
  }

  protected void start() {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("DW START");
    }

    startStopThread.schedule(new StartStop(StartStopAction.START), 10, TimeUnit.SECONDS);
  }

  protected void stop() {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("DW STOP");
    }

    startStopThread.submit(new StartStop(StartStopAction.STOP));
  }

  protected void restart() {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("DW RESTART");
    }

    startStopThread.submit(new StartStop(StartStopAction.RESTART));
  }

  private void init() {
    if (!initialized) {

      dw.getJersey().setUrlPattern(JERSEY_ENDPOINT);

      this.server = dw.getConfiguration().getServerFactory().build(dw.getEnvironment());

      //TODO ServletRegistration.Dynamic servlet = dw.getServlets().addServlet("osgi", new ProxyServlet());
      // servlet.addMapping(APP_ENDPOINT);

      addAdminEndpoint();

      this.initialized = true;
    }
  }

  private void addAdminEndpoint() {
    ServletHolder[] admin = dw.getEnvironment().getAdminContext().getServletHandler().getServlets();

    int ai = -1;
    for (int i = 0; i < admin.length; i++) {
      if (admin[i].getName().contains("Admin")) {
        ai = i;
      }
    }
    if (ai >= 0) {
      String name = admin[ai].getName();
      admin[ai] = new NonblockingServletHolder(adminEndpoint);
      admin[ai].setName(name);

      dw.getEnvironment().getAdminContext().getServletHandler().setServlets(admin);
    }
  }

  private void cleanup() {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("DW CLEANUP");
    }
    if (!initialized) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("DW CLEANUP 1");
      }
      init();
      // cleanup servletContext
      ServletContext servletContext = dw.getApplicationContext().getServletContext();
      //servletContext.setAttribute(BundleContext.class.getName(), context);
    } else {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("DW CLEANUP 2");
      }
      // cleanup servletContext
      ServletContext servletContext = dw.getApplicationContext().getServletContext();
      //servletContext.setAttribute(BundleContext.class.getName(), context);
      // cleanup metrics and jersey
      dw.resetServer();
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("DW CLEANUP 3");
    }

    // cleanup url
    this.url = "";
  }

  public String getUrl() {
    if (url.isEmpty()) {
      this.url = dw.getUri().toString();
    }
    return url;
  }
}
