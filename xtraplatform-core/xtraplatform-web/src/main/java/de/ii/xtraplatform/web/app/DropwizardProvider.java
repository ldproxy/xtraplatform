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
import com.codahale.metrics.health.HealthCheck;
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
import de.ii.xtraplatform.web.domain.DropwizardPlugin;
import de.ii.xtraplatform.web.domain.MustacheRenderer;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

//TODO: merge into AppLauncher
/** @author zahnen */
@Singleton
@AutoBind
public class DropwizardProvider implements AppLifeCycle {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DropwizardProvider.class);
  static final String JERSEY_ENDPOINT = "/rest/*";

  private final ApplicationProvider applicationProvider;
  private final AppContext appContext;
  private final MustacheRenderer mustacheRenderer;
  private final Lazy<Set<DropwizardPlugin>> plugins;

  private AppConfiguration configuration;
  private Environment environment;

  @Inject
  public DropwizardProvider(
      ApplicationProvider applicationProvider,
      MustacheRenderer mustacheRenderer,
      AppContext appContext,
      Lazy<Set<DropwizardPlugin>> plugins) {
    this.applicationProvider = applicationProvider;
    this.mustacheRenderer = mustacheRenderer;
    this.appContext = appContext;
    this.plugins = plugins;
  }

  @Override
  public int getPriority() {
    // start first
    return 0;
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
  }

  @Override
  public void onStop() {
  }

  private void init(Path cfgFilePath, ENV env) {
    Pair<AppConfiguration, Environment> configurationEnvironmentPair =
        applicationProvider.startWithFile(cfgFilePath, env, this::initBootstrap);

    this.configuration = configurationEnvironmentPair.getLeft();
    this.environment = configurationEnvironmentPair.getRight();

    this.environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    this.environment
        .healthChecks()
        .register(
            "store",
            new HealthCheck() {
              @Override
              protected Result check() throws Exception {
                return Result.healthy();
              }
            });

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

    plugins.get().stream()
        .sorted(Comparator.comparingInt(DropwizardPlugin::getPriority))
        .forEach(plugin -> plugin.init(configuration, environment));
  }

  //TODO: to plugin
  private void initBootstrap(Bootstrap<AppConfiguration> bootstrap) {
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
}
