/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.dropwizard.cfg;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.xtraplatform.dropwizard.api.ApplicationProvider;
import de.ii.xtraplatform.dropwizard.api.Dropwizard;
import de.ii.xtraplatform.dropwizard.api.XtraPlatformConfiguration;
import de.ii.xtraplatform.dropwizard.views.FallbackMustacheViewRenderer;
import de.ii.xtraplatform.dropwizard.views.MustacheResolverRegistry;
import de.ii.xtraplatform.runtime.FelixRuntime.ENV;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.jetty.MutableServletContextHandler;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import io.dropwizard.views.ViewRenderer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.annotations.Validate;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import static de.ii.xtraplatform.runtime.FelixRuntime.ENV.DEVELOPMENT;
import static de.ii.xtraplatform.runtime.FelixRuntime.ENV_KEY;
import static de.ii.xtraplatform.runtime.FelixRuntime.USER_CONFIG_PATH_KEY;


/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class DropwizardProvider implements Dropwizard {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DropwizardProvider.class);

    // Service not published by default
    @ServiceController(value = false)
    private boolean controller;


    private final BundleContext context;
    private final ApplicationProvider<XtraPlatformConfiguration> applicationProvider;
    private final MustacheResolverRegistry mustacheResolverRegistry;

    private XtraPlatformConfiguration configuration;
    private Environment environment;
    private ServletContainer jerseyContainer;
    private ViewRenderer mustacheRenderer;

    public DropwizardProvider(@Context BundleContext context,
                              @Requires ApplicationProvider<XtraPlatformConfiguration> applicationProvider,
                              @Requires MustacheResolverRegistry mustacheResolverRegistry) {
        this.context = context;
        this.applicationProvider = applicationProvider;
        this.mustacheResolverRegistry = mustacheResolverRegistry;
    }

    @Validate
    public void start() {
        Path cfgFile = Paths.get(context.getProperty(USER_CONFIG_PATH_KEY));
        ENV env = ENV.valueOf(context.getProperty(ENV_KEY));

        try {
            start(cfgFile, env);

            // publish the service once the initialization
            // is completed.
            controller = true;

            LOGGER.debug("Initialized XtraPlatform with configuration file {}", cfgFile);

        } catch (Throwable ex) {
            LOGGER.error("Error initializing XtraPlatform with configuration file {}", cfgFile, ex);
            System.exit(1);
        }
    }

    private void start(Path cfgFilePath, ENV env) {
        Pair<XtraPlatformConfiguration, Environment> configurationEnvironmentPair = applicationProvider.startWithFile(cfgFilePath, env, this::initBootstrap);

        this.configuration = configurationEnvironmentPair.getLeft();
        this.environment = configurationEnvironmentPair.getRight();
        this.jerseyContainer = (ServletContainer) environment.getJerseyServletContainer();

        //this.environment.healthChecks().register("ModulesHealthCheck", new ModulesHealthCheck());

        // TODO: enable trailing slashes, #36
        //environment.jersey().enable(ResourceConfig.FEATURE_REDIRECT);
        this.environment.getObjectMapper()
                        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        //TODO: per parameter
        //if (configuration.useFormattedJsonOutput) {
        environment.getObjectMapper()
                   .enable(SerializationFeature.INDENT_OUTPUT);
        //LOGGER.warn(FrameworkMessages.GLOBALLY_ENABLED_JSON_PRETTY_PRINTING);
        //}

        LOGGER.info("Store mode: {}", configuration.store.mode);
    }

    //@Override
    private void initBootstrap(Bootstrap<XtraPlatformConfiguration> bootstrap) {
        this.mustacheRenderer = new FallbackMustacheViewRenderer(mustacheResolverRegistry);

        boolean cacheTemplates = ENV.valueOf(context.getProperty(ENV_KEY)) != DEVELOPMENT;

        bootstrap.addBundle(new ViewBundle<XtraPlatformConfiguration>(ImmutableSet.of(mustacheRenderer)) {
            @Override
            public Map<String, Map<String, String>> getViewConfiguration(XtraPlatformConfiguration configuration) {
                return ImmutableMap.of(
                        mustacheRenderer.getConfigurationKey(),
                        ImmutableMap.of(
                                "cache",
                                Boolean.toString(cacheTemplates)
                        )
                );
            }
        });
    }

    @Override
    public XtraPlatformConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }


    /*@Override
    public Map<String, Boolean> getFlags() {
        Map<String, Boolean> flags = new HashMap<>();

        flags.put(FLAG_ALLOW_SERVICE_READDING, getConfiguration().allowServiceReAdding);
        flags.put(FLAG_USE_FORMATTED_JSON_OUTPUT, getConfiguration().useFormattedJsonOutput);

        return flags;
    }*/

    @Override
    public ServletEnvironment getServlets() {
        return getEnvironment().servlets();
    }

    @Override
    public ServletContext getServletContext() {
        return getEnvironment().getApplicationContext()
                               .getServletContext();
    }

    @Override
    public MutableServletContextHandler getApplicationContext() {
        return getEnvironment().getApplicationContext();
    }

    @Override
    public JerseyEnvironment getJersey() {
        return getEnvironment().jersey();
    }

    @Override
    public ServletContainer getJerseyContainer() {
        return jerseyContainer;
    }

    @Override
    public String getUrl() {
        return "http://" + getHostName() + ":" + String.valueOf(getApplicationPort()) + "/";
    }

    private int getApplicationPort() {
        return ((HttpConnectorFactory) ((DefaultServerFactory) getConfiguration().getServerFactory()).getApplicationConnectors()
                                                                                                     .get(0)).getPort();
    }

    private String getHostName() {
        String hostName = "";
        try {
            InetAddress iAddress = InetAddress.getLocalHost();
            hostName = iAddress.getCanonicalHostName();
            if (Objects.isNull(hostName) || hostName.isEmpty()) {
                hostName = iAddress.getHostName();
            }
        } catch (UnknownHostException e) {
            // failed;  try alternate means.
        }
        // windows environment variable
        if (Objects.isNull(hostName) || hostName.isEmpty()) {
            hostName = System.getenv("COMPUTERNAME");
        }
        // linux environment variable
        if (Objects.isNull(hostName) || hostName.isEmpty()) {
            hostName = System.getenv("HOSTNAME");
        }
        // last option
        if (Objects.isNull(hostName) || hostName.isEmpty()) {
            hostName = "localhost";
        }

        return hostName;
    }

    /*@Override
    public int getDebugLogMaxMinutes() {
        return getConfiguration().maxDebugLogDurationMinutes;
    }*/

    @Override
    public void attachLoggerAppender(Appender appender) {
        /*if (!appender.isStarted()) {
            appender.setContext(ROOT_LOGGER.getLoggerContext());
            appender.start();
        }
        ROOT_LOGGER.addAppender(appender);*/
    }

    @Override
    public void detachLoggerAppender(Appender appender) {
        //ROOT_LOGGER.detachAppender(appender);
        //appender.stop();
    }

    @Override
    public void setLoggingLevel(Level level) {
        // TODO: this only works for loggers whose level is not set explicitely in config
        //ROOT_LOGGER.setLevel(level);
    }

    @Override
    public void resetServer() {
        // cleanup metrics
        for (String name : environment.metrics()
                                      .getNames()) {
            if (name.contains("jetty")) {
                environment.metrics()
                           .remove(name);
            }
        }

        // cleanup jersey
        // still not sure why this is needed and why it works
        // if we don't do this, we get exceptions from the jersey thread local stack
        // that handles context injections for singleton resource classes
        // the jersey ServletContainer is a thin servlet wrapper around the resource config
        // we create a new instance of ServletContainer and replace the old one in the jetty config
        // BUT: if we destroy the old one and/or replace the reference that JaxRsRegistry uses for reloads,
        // we get exceptions again
        for (ServletHolder sh : getApplicationContext().getServletHandler()
                                                       .getServlets()) {
            if (sh.getName()
                  .contains("jersey")) {
                LOGGER.debug("JERSEY CLEANUP");
                ServletContainer sc = new ServletContainer(environment.jersey()
                                                                      .getResourceConfig());
                sh.setServlet(sc);

                //this.jerseyContainer.reload();
                //this.jerseyContainer.destroy();
                //this.jerseyContainer = sc;                
            }
        }
    }

    @Override
    public ViewRenderer getMustacheRenderer() {
        return mustacheRenderer;
    }
}
