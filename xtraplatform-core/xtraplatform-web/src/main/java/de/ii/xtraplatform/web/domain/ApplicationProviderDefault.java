/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import de.ii.xtraplatform.base.domain.AppConfiguration;
import de.ii.xtraplatform.base.domain.Constants;
import de.ii.xtraplatform.base.domain.Constants.ENV;
import de.ii.xtraplatform.web.app.MergingSourceProvider;
import de.ii.xtraplatform.web.app.XtraplatformCommand;
import io.dropwizard.Application;
import io.dropwizard.cli.Cli;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.JarLocation;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: merge back into DropwizardProvider
@Singleton
@AutoBind
public class ApplicationProviderDefault implements ApplicationProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationProviderDefault.class);
  private static final String DW_CMD = "server";

  private final CompletableFuture<AppConfiguration> configuration;
  private final CompletableFuture<Environment> environment;
  private final String applicationName;
  private final String applicationVersion;
  private final ENV applicationEnvironment;

  @Inject
  public ApplicationProviderDefault() {
    this.configuration = new CompletableFuture<>();
    this.environment = new CompletableFuture<>();
    this.applicationName = "TODO"; // context.getProperty(Constants.APPLICATION_KEY);
    this.applicationVersion = "TODO"; // context.getProperty(Constants.VERSION_KEY);
    this.applicationEnvironment =
        ENV.DEVELOPMENT; // TODO.valueOf(context.getProperty(Constants.ENV_KEY));
  }

  public Optional<ByteSource> getConfigurationFileTemplate(String environment) {
    return getConfigurationFileTemplateFromClassBundle(
        environment, ApplicationProviderDefault.class);
  }

  private Optional<ByteSource> getConfigurationFileTemplateFromClassBundle(
      String environment, Class<?> clazz) {
    String cfgFileTemplateName = String.format("/cfg.%s.yml", environment);
    ByteSource byteSource = null;
    try {
      byteSource = Resources.asByteSource(Resources.getResource(clazz, cfgFileTemplateName));
    } catch (Throwable e) {
      // ignore
    }
    return Optional.ofNullable(byteSource);
  }

  @Override
  public Pair<AppConfiguration, Environment> startWithFile(
      Path configurationFile,
      Constants.ENV env,
      Consumer<Bootstrap<AppConfiguration>> initializer) {
    Bootstrap<AppConfiguration> bootstrap = getBootstrap(initializer, env);

    return run(configurationFile.toString(), bootstrap);
  }

  private Pair<AppConfiguration, Environment> run(
      String configurationFilePath, Bootstrap<AppConfiguration> bootstrap) {
    final Cli cli = new Cli(new JarLocation(getClass()), bootstrap, System.out, System.err);
    String[] arguments = {DW_CMD, configurationFilePath};

    try {
      if (cli.run(arguments).isEmpty()) {
        AppConfiguration cfg = configuration.get(30, TimeUnit.SECONDS);
        Environment env = environment.get(30, TimeUnit.SECONDS);

        return new ImmutablePair<>(cfg, env);
      }
    } catch (Exception e) {
      // continue
    }

    throw new IllegalStateException();
  }

  private Bootstrap<AppConfiguration> getBootstrap(
      Consumer<Bootstrap<AppConfiguration>> initializer, Constants.ENV env) {
    Application<AppConfiguration> application =
        new Application<AppConfiguration>() {
          @Override
          public void run(AppConfiguration configuration, Environment environment)
              throws Exception {
            ApplicationProviderDefault.this.configuration.complete(configuration);
            ApplicationProviderDefault.this.environment.complete(environment);
          }
        };

    Bootstrap<AppConfiguration> bootstrap = new Bootstrap<>(application);
    bootstrap.addCommand(new XtraplatformCommand<>(application));

    bootstrap.setConfigurationSourceProvider(
        new SubstitutingSourceProvider(
            new MergingSourceProvider(
                bootstrap.getConfigurationSourceProvider(), ImmutableMap.of(), env),
            new EnvironmentVariableSubstitutor(false)));

    initializer.accept(bootstrap);

    bootstrap.registerMetrics();

    return bootstrap;
  }
}
