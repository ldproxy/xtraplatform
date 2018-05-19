/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.server;

import de.ii.xsf.core.api.session.SessionManager;
import de.ii.xsf.dropwizard.api.Dropwizard;
import de.ii.xsf.dropwizard.cfg.FakeResource;
import de.ii.xsf.dropwizard.cfg.QueryParamConnegFilter;
import de.ii.xsf.logging.XSFLogger;
import org.apache.felix.http.proxy.ProxyServlet;
import org.apache.felix.ipojo.annotations.*;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.MultiException;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.glassfish.jersey.server.ResourceConfig;
import org.osgi.framework.BundleContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author zahnen
 */
@Component(immediate = true, publicFactory = false)
@Instantiate
public class WebServerDropwizard {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(WebServerDropwizard.class);
    private static final String APP_ENDPOINT = "/*";
    private static final String JERSEY_ENDPOINT = "/rest/*";

    @Context
    private BundleContext context;

    // wait for logging
    //@Requires
    //private LogStore logStore;

    @Requires
    private Dropwizard dw;

    private boolean initialized;
    private Server server;
    private SessionManager sessionManager;
    private String url;

    private boolean started;
    private final Lock startStopLock;
    private final ExecutorService startStopThread;

    public WebServerDropwizard() {

        this.url = "";

        this.sessionManager = null;

        this.startStopLock = new ReentrantLock();

        this.startStopThread = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
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
                    try {
                        String u = getUrl();

                        server.stop();
                        server.join();

                        started = false;

                        LOGGER.getLogger().info("Stopped web server at {}", u);
                        Thread.sleep(1000);

                    } catch (MultiException ex) {
                        for (Throwable t : ex.getThrowables()) {
                            if (t != null) {
                                LOGGER.getLogger().error("Error stopping web server: {}", t.getMessage());
                                LOGGER.getLogger().debug("Error stopping web server", t);
                            }
                        }
                    } catch (Exception ex) {
                        LOGGER.getLogger().error("Error stopping web server: {}", ex.getMessage());
                        LOGGER.getLogger().debug("Error stopping web server", ex);
                    }
                }
                if (!started && (action == StartStopAction.START || action == StartStopAction.RESTART)) {
                    LOGGER.getLogger().debug("DW START {}", Thread.currentThread().getName());
                    cleanup();

                    try {
                        server.start();

                        started = true;

                        LOGGER.getLogger().info("Started web server at {}", getUrl());

                    } catch (Exception ex) {
                        LOGGER.getLogger().error("Error starting web server: {}", ex.getMessage());
                        LOGGER.getLogger().debug("Error starting web server", ex);
                    }
                }
            } finally {
                startStopLock.unlock();
            }
        }
    }

    @Validate
    protected void startBundle() {
        LOGGER.getLogger().debug("DW STARTBUNDLE");

        start();
    }

    @Invalidate
    protected void stopBundle() {
        LOGGER.getLogger().debug("DW STOPBUNDLE");

        stop();

        startStopThread.shutdownNow();
    }

    protected void start() {
        LOGGER.getLogger().debug("DW START");

        startStopThread.submit(new StartStop(StartStopAction.START));
    }

    protected void stop() {
        LOGGER.getLogger().debug("DW STOP");

        startStopThread.submit(new StartStop(StartStopAction.STOP));
    }

    protected void restart() {
        LOGGER.getLogger().debug("DW RESTART");

        startStopThread.submit(new StartStop(StartStopAction.RESTART));
    }

    private void init() {
        if (!initialized) {
            LOGGER.getLogger().info("-----------------------------------------------------");

            dw.getJersey().setUrlPattern(JERSEY_ENDPOINT);

            //dw.getJersey().register(new FakeResource());

            // TODO: verify
            dw.getJersey().getResourceConfig().register(QueryParamConnegFilter.class);
            //dw.getJersey().getResourceConfig().getContainerRequestFilters().add(QueryParamConnegFilter.class);

            // TODO: no longer part of jersey, replace with: https://stackoverflow.com/questions/23600676/how-to-normalize-uris-in-jersey-2
            //dw.getJersey().enable(ResourceConfig.FEATURE_CANONICALIZE_URI_PATH);

            this.server = dw.getConfiguration().getServerFactory().build(dw.getEnvironment());

            ServletRegistration.Dynamic servlet = dw.getServlets().addServlet("osgi", new ProxyServlet());
            servlet.addMapping(APP_ENDPOINT);

            /*dw.getEnvironment().getAdminContext().destroy();
            for (Connector c : server.getConnectors()) {
                if (c.getName().equals("admin")) {
                    try {
                        c.stop();
                        server.removeConnector(c);
                    } catch (Exception ex) {
                        LOGGER.getLogger().debug("Error removing connector {}", c.getName(), ex);
                    }
                }
            }*/

            this.initialized = true;
        }
    }

    private void cleanup() {
        LOGGER.getLogger().debug("DW CLEANUP");
        if (!initialized) {
            LOGGER.getLogger().debug("DW CLEANUP 1");
            init();
            // cleanup servletContext
            ServletContext servletContext = dw.getApplicationContext().getServletContext();
            servletContext.setAttribute(BundleContext.class.getName(), context);
        } else {
            LOGGER.getLogger().debug("DW CLEANUP 2");
            // cleanup servletContext
            ServletContext servletContext = dw.getApplicationContext().getServletContext();
            servletContext.setAttribute(BundleContext.class.getName(), context);
            // cleanup metrics and jersey
            dw.resetServer();
        }

        LOGGER.getLogger().debug("DW CLEANUP 3");

        // cleanup session manager
        /*if (this.sessionManager == null) {
            disableSessionManager();
        } else {
            enableSessionManager(sessionManager);
        }*/

        // cleanup url
        this.url = "";
    }

    // TODO: reimplement
    /*@Bind(optional = true, policy = BindingPolicy.DYNAMIC_PRIORITY)
    public void bind(SessionManager sm) {
        LOGGER.getLogger().debug("DW BIND");

        this.sessionManager = sm;

        restart();
    }

    @Unbind
    public void unbind(SessionManager sm) {
        LOGGER.getLogger().debug("DW UNBIND");

        this.sessionManager = null;

        restart();
    }*/

    /*private void enableSessionManager(SessionManager sm) {
        if (!(sm instanceof Nullable || sm == null)) {
            LOGGER.getLogger().debug("enableSessionManager: {}", sm);

            org.eclipse.jetty.server.SessionManager sm2 = sm.getSessionManager();
            dw.getApplicationContext().setSessionHandler(new SessionHandler(sm2));
            dw.getApplicationContext().setSessionsEnabled(true);
            dw.getApplicationContext().getServer().setSessionIdManager(sm.getSessionIdManager());
        }
    }

    private void disableSessionManager() {
        LOGGER.getLogger().debug("disableSessionManager");

        dw.getApplicationContext().setSessionHandler(null);
        dw.getApplicationContext().setSessionsEnabled(false);
        dw.getApplicationContext().getServer().setSessionIdManager(null);
    }*/

    public String getUrl() {
        if (url.isEmpty()) {
            if (dw.hasExternalUrl()) {
                this.url = dw.getExternalUrl();
            } else {
                this.url = "http://" + dw.getHostName() + ":" + String.valueOf(dw.getApplicationPort()) + "/";
            }
        }
        return url;
    }

}
