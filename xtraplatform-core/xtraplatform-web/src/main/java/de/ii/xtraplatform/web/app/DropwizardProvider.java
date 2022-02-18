/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import dagger.Lazy;
import de.ii.xtraplatform.base.domain.AppConfiguration;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.Constants.ENV;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.web.domain.ApplicationProvider;
import de.ii.xtraplatform.web.domain.Dropwizard;
import de.ii.xtraplatform.web.domain.DropwizardPlugin;
import de.ii.xtraplatform.web.domain.MustacheResolverRegistry;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jetty.MutableServletContextHandler;
import io.dropwizard.jetty.NonblockingServletHolder;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import io.dropwizard.views.ViewRenderer;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;
import javax.servlet.ServletContext;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.LoggerFactory;

/** @author zahnen */
@Singleton
@AutoBind
public class DropwizardProvider implements Dropwizard, AppLifeCycle {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DropwizardProvider.class);
  static final String JERSEY_ENDPOINT = "/rest/*";

  private final ApplicationProvider applicationProvider;
  private final MustacheResolverRegistry mustacheResolverRegistry;
  private final AdminEndpointServlet adminEndpoint;
  private final AppContext appContext;
  private final Lazy<Set<DropwizardPlugin>> plugins;

  private AppConfiguration configuration;
  private Environment environment;
  private ServletContainer jerseyContainer;
  private ViewRenderer mustacheRenderer;
  private Server server;

  @Inject
  public DropwizardProvider(
      ApplicationProvider applicationProvider,
      MustacheResolverRegistry mustacheResolverRegistry,
      AppContext appContext,
      AdminEndpointServlet adminEndpoint,
      Lazy<Set<DropwizardPlugin>> plugins) {
    this.applicationProvider = applicationProvider;
    this.mustacheResolverRegistry = mustacheResolverRegistry;
    this.appContext = appContext;
    this.adminEndpoint = adminEndpoint;
    this.plugins = plugins;
  }

  @Override
  public int getPriority() {
    // start last
    return 2000;
  }

  @Override
  public void onStart() {
    Thread.currentThread().setName("startup");

    Path cfgFile = appContext.getConfigurationFile();

    try {
      init(cfgFile, appContext.getEnvironment());
    } catch (Throwable ex) {
      LogContext.error(
          LOGGER,
          ex,
          "Error initializing {} with configuration file {}",
          appContext.getName(),
          cfgFile);
      System.exit(1);
    }

    try {
      run();
      LOGGER.info("Started web server at {}", appContext.getUri());
    } catch (Throwable ex) {
      LogContext.error(LOGGER, ex, "Error starting {}", appContext.getName());
      System.exit(1);
    }
  }

  @Override
  public void onStop() {
    try {
      server.stop();
      server.join();
    } catch (Exception e) {
      LogContext.error(LOGGER, e, "Error when stopping web server");
    }
  }

  private void run() throws Exception {
    environment.jersey().setUrlPattern(JERSEY_ENDPOINT);

    this.server = configuration.getServerFactory().build(environment);

    addAdminEndpoint();

    server.start();
  }

  private void addAdminEndpoint() {
    ServletHolder[] admin = environment.getAdminContext().getServletHandler().getServlets();

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

      environment.getAdminContext().getServletHandler().setServlets(admin);
    }
  }

  private void init(Path cfgFilePath, ENV env) {
    Pair<AppConfiguration, Environment> configurationEnvironmentPair =
        applicationProvider.startWithFile(cfgFilePath, env, this::initBootstrap);

    this.configuration = configurationEnvironmentPair.getLeft();
    this.environment = configurationEnvironmentPair.getRight();
    this.jerseyContainer = (ServletContainer) environment.getJerseyServletContainer();

    // this.environment.healthChecks().register("ModulesHealthCheck", new ModulesHealthCheck());

    this.environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    // TODO: per parameter
    environment.getObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    environment
        .metrics()
        .removeMatching(
            new MetricFilter() {
              @Override
              public boolean matches(String name, Metric metric) {
                if (name.startsWith("jvm.memory.pools") || name.startsWith("ch.qos.logback")) {
                  return true;
                }
                return false;
              }
            });

    for (DropwizardPlugin plugin : plugins.get()) {
      plugin.init(configuration, environment);
    }
  }

  private void initBootstrap(Bootstrap<AppConfiguration> bootstrap) {
    this.mustacheRenderer = new FallbackMustacheViewRenderer(mustacheResolverRegistry);

    boolean cacheTemplates = !appContext.isDevEnv();

    bootstrap.addBundle(
        new ViewBundle<>(ImmutableSet.of(mustacheRenderer)) {
          @Override
          public Map<String, Map<String, String>> getViewConfiguration(
              AppConfiguration configuration) {
            return ImmutableMap.of(
                mustacheRenderer.getConfigurationKey(),
                ImmutableMap.of("cache", Boolean.toString(cacheTemplates)));
          }
        });
  }

  @Override
  public Environment getEnvironment() {
    return environment;
  }

  @Override
  public ServletEnvironment getServlets() {
    return getEnvironment().servlets();
  }

  @Override
  public ServletContext getServletContext() {
    return getEnvironment().getApplicationContext().getServletContext();
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
  public ViewRenderer getMustacheRenderer() {
    return mustacheRenderer;
  }
}
