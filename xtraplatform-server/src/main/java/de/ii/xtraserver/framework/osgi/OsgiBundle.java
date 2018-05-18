/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraserver.framework.osgi;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import de.ii.xsf.dropwizard.cfg.XtraServerFrameworkConfiguration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.http.proxy.ProxyServlet;
import org.apache.felix.main.AutoProcessor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author zahnen
 */
@Deprecated
public class OsgiBundle implements ConfiguredBundle<XtraServerFrameworkConfiguration>, Managed, BundleActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsgiBundle.class);
    private static final Map<String, String> exports = new ImmutableMap.Builder<String, String>()
            .put("javax.servlet", "3.0.0")
            .put("javax.servlet.http", "3.0.0")
            .put("javax.servlet.descriptor", "3.0.0")
            .put("org.slf4j", "1.7.6")
            .put("org.json", "0.0.0.20070829")
            .put("com.google.common.base", "16.0")
            .put("com.google.common.primitives", "16.0")
            .put("com.google.common.net", "16.0")
            .put("com.google.common.hash", "16.0")
            .put("com.fasterxml.jackson.annotation", "2.3")
            .put("com.fasterxml.jackson.databind", "2.3")
            .put("com.fasterxml.jackson.databind.util", "2.3")
            .put("javax.ws.rs", "1.1")
            .put("javax.ws.rs.core", "1.1")
            .put("javax.ws.rs.ext", "1.1")
            .put("de.ii.xtraserver.framework.api", "0.9")
            .put("de.ii.xtraserver.framework.util", "0.9")
            .put("io.dropwizard.auth", "0.7")
            .put("io.dropwizard.auth.oauth", "0.7")
            .put("io.dropwizard.views", "0.7")
            .put("io.dropwizard.jersey.caching", "0.7")
            .put("io.dropwizard.jersey.setup", "0.7")
            .put("io.dropwizard.jetty", "0.7")
            .put("io.dropwizard.jetty.setup", "0.7")
            .put("io.dropwizard.views.mustache", "0.7")
            .put("io.dropwizard.servlets.assets", "0.7")
            .put("org.eclipse.jetty.util", "0.7")
            .put("org.eclipse.jetty.util.component", "0.7")
            .put("org.eclipse.jetty.server", "0.7")
            .put("org.eclipse.jetty.server.session", "0.7")
            .put("org.eclipse.jetty.server.handler", "0.7")
            .put("com.sun.jersey.api.core", "1.18")
            // TODO: for auth2 module, check if really needed
            ///.put("com.fasterxml.jackson.module.mrbean", "2.3")
            ///.put("org.mapdb", "0.9")
            .put("com.sun.jersey.spi.inject", "1.18")
            .put("com.sun.jersey.spi.container", "1.18")
            .put("com.sun.jersey.spi.container.servlet", "1.18")
            .put("com.sun.jersey.api.model", "1.18")
            .put("com.sun.jersey.core.spi.component", "1.18")
            .put("com.sun.jersey.server.impl.inject", "1.18")
            .put("com.sun.jersey.api.client.filter", "1.18")
            .put("com.sun.jersey.api.client", "1.18")
            .put("com.sun.jersey.api.client.config", "1.18")
            .put("com.sun.jersey.core.header", "1.18")
            .put("javax.validation", "1.1.0")
            .put("org.apache.commons.codec.binary", "1.6")
            .put("org.apache.commons.lang", "2.6")
            .put("org.apache.commons.lang.builder", "2.6")
            .put("org.apache.http.client.utils", "4.3.2")
            .put("org.apache.http.client.config", "4.3.2")
            .put("org.apache.http.client.entity", "4.3.2")
            .put("org.apache.http.client.protocol", "4.3.2")
            // TODO: for wfs2gsfs module, check if really needed or should be embedded
            .put("com.fasterxml.jackson.core", "2.3")
            .put("com.fasterxml.jackson.core.io", "2.3")
            .put("com.fasterxml.jackson.databind.introspect", "2.3")
            .put("com.fasterxml.jackson.databind.node", "2.3")
            .put("com.google.common.io", "16.0")
            ///.put("com.sun.xml.xsom", "20110809")
            ///.put("com.sun.xml.xsom.parser", "20110809")
            ///.put("net.sf.jsqlparser", "0.8.0")
            ///.put("net.sf.jsqlparser.expression", "0.8.0")
            ///.put("net.sf.jsqlparser.expression.operators.arithmetic", "0.8.0")
            ///.put("net.sf.jsqlparser.expression.operators.conditional", "0.8.0")
            ///.put("net.sf.jsqlparser.expression.operators.relational", "0.8.0")
            ///.put("net.sf.jsqlparser.parser", "0.8.0")
            ///.put("net.sf.jsqlparser.schema", "0.8.0")
            ///.put("net.sf.jsqlparser.statement", "0.8.0")
            //.put("net.sf.jsqlparser.statement.select", "0.8.0")
            .put("org.apache.commons.beanutils", "1.8.3")
            .put("org.apache.http", "4.3.2")
            .put("org.apache.http.client", "4.3.2")
            .put("org.apache.http.client.methods", "4.3.2")
            .put("org.apache.http.entity", "4.3.2")
            .put("org.apache.http.message", "4.3.2")
            .put("org.apache.http.params", "4.3.2")
            .put("org.apache.http.protocol", "4.3.2")
            .put("org.apache.http.util", "4.3.2")
            .put("org.apache.http.impl.client", "4.3.2")
            ///.put("org.codehaus.stax2", "3.1.4")
            ///.put("org.codehaus.staxmate", "2.0.1")
            ///.put("org.codehaus.staxmate.in", "2.0.1")
            .put("org.joda.time", "2.3")
            .put("org.joda.time.format", "2.3")
            // for map.apps
            .put("javax.net.ssl", "0.0")
            .put("javax.net", "0.0")
            .put("sun.misc", "0.0")
            // ELASTICSEARCH hier pakete die benutzt werden sollen
            /*.put("org.elasticsearch", "1.2.0")
            .put("org.elasticsearch.node", "1.2.0")
            .put("org.elasticsearch.action", "1.2.0")
            .put("org.elasticsearch.action.index", "1.2.0")
            .put("org.elasticsearch.action.get", "1.2.0")
            .put("org.elasticsearch.action.search", "1.2.0")
            .put("org.elasticsearch.index.query", "1.2.0")
            .put("org.elasticsearch.client", "1.2.0")
            .put("org.elasticsearch.indices", "1.2.0")
            .put("org.elasticsearch.action.admin.indices.create", "1.2.0")
            .put("org.elasticsearch.action.admin.indices.exists.indices", "1.2.0")
            .put("org.elasticsearch.search", "1.2.0")
            .put("org.elasticsearch.common.settings", "1.2.0")
            .put("org.elasticsearch.action.admin.cluster.node.shutdown", "1.2.0")
            .put("org.elasticsearch.action.count", "1.2.0")
            .put("org.elasticsearch.action.delete", "1.2.0")
            .put("org.elasticsearch.action.admin.indices.refresh", "1.2.0")
            .put("org.elasticsearch.action.admin.indices.mapping.put", "1.2.0")
            .put("org.elasticsearch.search.sort", "1.2.0")
            .put("org.elasticsearch.action.admin.indices.delete", "1.2.0")
            .put("org.elasticsearch.action.bulk", "1.2.0")
            .put("org.elasticsearch.action.admin.indices.mapping.delete", "1.2.0")
            .put("org.elasticsearch.action.admin.indices.flush", "1.2.0")
            */
            // Logback
            .put("ch.qos.logback.classic", "1.1.1")
            .put("ch.qos.logback.classic.spi", "1.1.1")
            .put("ch.qos.logback.classic.encoder", "1.1.1")
            .put("ch.qos.logback.core", "1.1.1")
            .put("ch.qos.logback.core.encoder", "1.1.1")
            .put("ch.qos.logback.core.filter", "1.1.1")
            .put("ch.qos.logback.core.spi", "1.1.1")
            .put("org.osgi.service.event", "1.5")
            .build();

    private ServletContext servletContext = null;
    private Map felixConfig = new HashMap();
    private File cfgDir;
    private String osgiEndpoint;
    private Felix felix = null;
    private Environment env = null;
    //private JAXRSWhiteboard tracker = null;
    //private XSFGlobalConfiguration xsfCfg;
    private ServiceRegistration m_registration = null;

    public OsgiBundle(File cfgDir, String osgiEndpoint) {
        this.cfgDir = cfgDir;
        this.osgiEndpoint = osgiEndpoint;
    }

    /*public XSFGlobalConfiguration getXsfCfg() {
        return xsfCfg;
    }*/

    @Override
    public void run(XtraServerFrameworkConfiguration xsfConfig, Environment e) throws Exception {
        servletContext = e.getApplicationContext().getServletContext();
        env = e;

        String bundleDir = new File(new File(cfgDir.getParentFile(), "bundles"), "runtime").getAbsolutePath();
        String levelOne = "";
        String levelTwo = "";
        for (File f : new File(bundleDir).listFiles()) {
            if (f.getName().endsWith(".jar")) {
                if (f.getName().startsWith("xsf-config-store") || f.getName().startsWith("xsf-core-sessions") || f.getName().startsWith("xsf-logging-inmemory") || f.getName().startsWith("xsf-logging-api") || !f.getName().startsWith("xsf-")) {
                    levelOne += "reference:file:" + f.getAbsolutePath().replaceAll(" ", "%20") + " ";
                } else {
                    levelTwo += "reference:file:" + f.getAbsolutePath().replaceAll(" ", "%20") + " ";
                }
            }
        }

        felixConfig.put(AutoProcessor.AUTO_START_PROP + ".1", levelOne);
        felixConfig.put(AutoProcessor.AUTO_START_PROP + ".2", levelTwo);
        felixConfig.put(FelixConstants.FRAMEWORK_BEGINNING_STARTLEVEL, "2");
        
        felixConfig.put("felix.fileinstall.dir", new File(new File(cfgDir.getParentFile(), "bundles"), "platform").getAbsolutePath());
        //felixConfig.put("felix.fileinstall.dir", new File("/home/fischer/Development/XtraServerFramework/target/bundles/platform").getAbsolutePath());
        felixConfig.put("felix.fileinstall.active.level", "1");
        felixConfig.put("felix.fileinstall.filter", "^xsf-.*");
        
        //felixConfig.put(AutoProcessor.AUTO_DEPLOY_DIR_PROPERY, bundleDir);
        //felixConfig.put(AutoProcessor.AUTO_DEPLOY_ACTION_PROPERY, "install,start");
        
        felixConfig.put(FelixConstants.FRAMEWORK_STORAGE, new File(cfgDir, "felix-cache").getAbsolutePath());

        // Export the host provided service interface package.
        felixConfig.put(FelixConstants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, Joiner.on(',').withKeyValueSeparator(";version=").join(exports));

        // Create host activator;
        List<BundleActivator> list = new ArrayList();
        list.add(this);
        felixConfig.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, list);
        
        //xsfCfg = new GlobalConfigurationService(xsfConfig, cfgDir, e);
        
        try {
            // Now create an instance of the framework with
            // our configuration properties.
            felix = new Felix(felixConfig);
            felix.init();
        } catch (Exception ex) {
            LOGGER.error("Could not create Felix OSGi runtime: " + ex);
        }
        
        // (9) Use the system bundle context to process the auto-deploy
        // and auto-install/auto-start properties.
        AutoProcessor.process(felixConfig, felix.getBundleContext());


        //LOGGER.info("SET_PROXY_SERVLET");
        ServletRegistration.Dynamic servlet = e.servlets().addServlet("osgi", new ProxyServlet());
        servlet.addMapping(osgiEndpoint);
    }

    public void waitForLoggingAndSessions(int timeout) {

        try {
            
            // Now start Felix instance.
            felix.start();
            
            for (int i = 0; i < timeout; i++) {
                try {
                    ServiceReference ref1 = felix.getBundleContext().getServiceReference("de.ii.xsf.logging.api.LogStore");
                    ServiceReference ref2 = felix.getBundleContext().getServiceReference("de.ii.xsf.core.api.web.SessionManager");

                    if (ref1 != null && ref2 != null) {
                        //System.out.println("FOUND LogStore and SessionManager");
                        
                        return;
                    }
                    //System.out.println("WAITING ...");
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            LOGGER.info("FELIX_STARTED");
        } catch (Exception ex) {
            LOGGER.error("Could not create Felix OSGi runtime: " + ex);
        }

    }

    @Override
    public void initialize(Bootstrap<?> btstrp) {
    }

    @Override
    public void start() throws Exception {
        
    }

    @Override
    public void stop() throws Exception {
        felix.stop();
        felix.waitForStop(0);
    }

    @Override
    public void start(BundleContext bc) throws Exception {
        //LOGGER.info("SET_BUNDLE_CONTEXT");
        
        servletContext.setAttribute(BundleContext.class.getName(), bc);

        //tracker = new JAXRSWhiteboard(bc, env);
        //tracker.open();

        //m_registration = bc.registerService(XSFGlobalConfiguration.class.getName(), xsfCfg, null);
        
    }

    @Override
    public void stop(BundleContext bc) throws Exception {
        //tracker.close();
        m_registration.unregister();
    }
}
