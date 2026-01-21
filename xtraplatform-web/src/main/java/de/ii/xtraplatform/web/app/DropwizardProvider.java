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
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.xml.JacksonJaxbXMLProvider;
import com.github.azahnen.dagger.annotations.AutoBind;
import dagger.Lazy;
import de.ii.xtraplatform.base.domain.AppConfiguration;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.ConfigurationReader;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.web.domain.DropwizardPlugin;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.core.Application;
import io.dropwizard.core.cli.Cli;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.util.JarLocation;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Validator;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class DropwizardProvider implements AppLifeCycle {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DropwizardProvider.class);
  private static final String[] DW_ARGS = {XtraplatformCommand.CMD, "cfg.yml"};
  static final String JERSEY_ENDPOINT = "/*";

  private final AppContext appContext;
  private final Lazy<Set<DropwizardPlugin>> plugins;

  @Inject
  public DropwizardProvider(AppContext appContext, Lazy<Set<DropwizardPlugin>> plugins) {
    this.appContext = appContext;
    this.plugins = plugins;
  }

  @Override
  public int getPriority() {
    // start first
    return 0;
  }

  @Override
  public CompletionStage<Void> onStart(boolean isStartupAsync) {
    // Thread.currentThread().setName("startup");

    try {
      init();
    } catch (Throwable ex) {
      LogContext.error(LOGGER, ex, "Error during initializing of {}", appContext.getName());
      return CompletableFuture.failedFuture(ex);
    }

    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void onStop() {}

  private void init() {
    Environment environment = initEnvironment();

    environment
        .getObjectMapper()
        .setSerializationInclusion(Include.NON_ABSENT)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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

    environment.jersey().setUrlPattern(JERSEY_ENDPOINT);

    environment.jersey().register(new JsonProviderOptionalPretty(environment.getObjectMapper()));
    environment.jersey().register(new JacksonJaxbXMLProvider());

    appContext.getConfiguration().getServerFactory().build(environment);

    plugins.get().stream()
        .sorted(Comparator.comparingInt(DropwizardPlugin::getPriority))
        .forEach(plugin -> plugin.init(appContext.getConfiguration(), environment));
  }

  private Environment initEnvironment() {
    CompletableFuture<Environment> environment = new CompletableFuture<>();
    Bootstrap<AppConfiguration> bootstrap = initBootstrap(environment);

    final Cli cli = new Cli(new JarLocation(getClass()), bootstrap, System.out, System.err);

    try {
      if (cli.run(DW_ARGS).isEmpty()) {
        return environment.get(30, TimeUnit.SECONDS);
      }
    } catch (Throwable e) {
      // continue
    }

    throw new IllegalStateException();
  }

  private Bootstrap<AppConfiguration> initBootstrap(
      CompletableFuture<Environment> futureEnvironment) {
    Application<AppConfiguration> application =
        new Application<>() {
          @Override
          public void run(AppConfiguration configuration, Environment environment)
              throws Exception {
            futureEnvironment.complete(environment);
          }
        };

    Bootstrap<AppConfiguration> bootstrap = new Bootstrap<>(application);
    bootstrap.addCommand(new XtraplatformCommand<>(application));

    ConfigurationReader configurationReader = new ConfigurationReader(Map.of());
    bootstrap.setConfigurationSourceProvider(
        ignore -> configurationReader.asInputStream(appContext.getConfiguration()));

    // NOTE: using bootstrap.setObjectMapper would change the ObjectMapper in environment, this just
    // uses our ObjectMapper to load cfg.yml
    bootstrap.setConfigurationFactoryFactory(
        new DefaultConfigurationFactoryFactory<>() {
          @Override
          public ConfigurationFactory<AppConfiguration> create(
              Class<AppConfiguration> klass,
              Validator validator,
              ObjectMapper objectMapper,
              String propertyPrefix) {
            return super.create(klass, validator, configurationReader.getMapper(), propertyPrefix);
          }
        });

    plugins.get().stream()
        .sorted(Comparator.comparingInt(DropwizardPlugin::getPriority))
        .forEach(plugin -> plugin.initBootstrap(bootstrap));

    bootstrap.registerMetrics();

    return bootstrap;
  }
}
