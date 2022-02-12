/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import static de.ii.xtraplatform.base.domain.Constants.TMP_DIR_PROP;

import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import de.ii.xtraplatform.base.domain.Constants.ENV;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author zahnen */
public class AppLauncher implements AppContext {
  private static final Logger LOGGER = LoggerFactory.getLogger(AppLauncher.class);

  private static final String ENV_VAR = "XTRAPLATFORM_ENV";
  private static final String DATA_DIR_NAME = "data";
  private static final String TMP_DIR_NAME = "tmp";

  private final String name;
  private final String version;
  private Constants.ENV env;
  private AppConfiguration cfg;

  public AppLauncher(String name, String version) {
    this.name = name;
    this.version = version;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public ENV getEnvironment() {
    return env;
  }

  @Override
  public AppConfiguration getConfiguration() {
    return cfg;
  }

  @Override
  public URI getUri() {
    if (Strings.isNullOrEmpty(getConfiguration().getServerFactory().getExternalUrl())) {
      return URI.create(
          String.format("%s://%s:%d", getScheme(), getHostName(), getApplicationPort()));
    }

    return URI.create(
        getConfiguration()
            .getServerFactory()
            .getExternalUrl()
            .replace("rest/services/", "")
            .replace("rest/services", ""));
  }

  public void init(String[] args, List<ByteSource> baseConfigs) throws IOException {
    Path dataDir =
        getDataDir(args).orElseThrow(() -> new IllegalArgumentException("No data directory found"));
    System.setProperty(TMP_DIR_PROP, dataDir.resolve(TMP_DIR_NAME).toAbsolutePath().toString());

    this.env = parseEnvironment();
    ConfigurationReader configurationReader = new ConfigurationReader(baseConfigs);
    Path cfgFile = configurationReader.getConfigurationFile(dataDir, env);

    configurationReader.loadMergedLogging(cfgFile, env);

    LOGGER.info("--------------------------------------------------");
    LOGGER.info("Starting {} v{}", name, version);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Data directory: {}", dataDir);
      LOGGER.debug("Environment: {}", env);
      LOGGER.debug("Base configurations: {}", baseConfigs);
      LOGGER.debug("User configurations: [{}]", cfgFile);
    }

    String cfgString = configurationReader.loadMergedConfigAsString(cfgFile, env);
    this.cfg = configurationReader.configFromString(cfgString);

    if (LOGGER.isDebugEnabled(LogContext.MARKER.DUMP)) {
      LOGGER.debug(LogContext.MARKER.DUMP, "Application configuration: \n{}", cfgString);
    }
  }

  public void start(App modules) {
    modules
        .lifecycle()
        .forEach(
            lifecycle -> {
              try {
                lifecycle.onStart();
              } catch (Exception ex) {
                // TODO: module name
                LogContext.error(LOGGER, ex, "Module startup error");
              }
            });
  }

  public void stop(App modules) {
    LOGGER.info("Shutting down {}", name);

    try {
      modules.lifecycle().forEach(Lifecycle::onStop);
    } catch (Exception ex) {
      // ignore
    }

    LOGGER.info("Stopped {}", name);
    LOGGER.info("--------------------------------------------------");
  }

  private Optional<Path> getDataDir(String[] args) {
    Path dataDir;

    if (args.length >= 1) {
      dataDir = Paths.get(args[0]);
    } else {
      dataDir = Paths.get(DATA_DIR_NAME).toAbsolutePath();
      if (!Files.isDirectory(dataDir)) {
        dataDir = Paths.get("../", DATA_DIR_NAME).toAbsolutePath();
      }
    }
    if (!Files.isDirectory(dataDir)) {
      return Optional.empty();
    }

    return Optional.of(dataDir);
  }

  private Constants.ENV parseEnvironment() {
    return Optional.ofNullable(System.getenv(ENV_VAR))
        .filter(
            e ->
                Arrays.stream(Constants.ENV.values())
                    .map(Enum::name)
                    .anyMatch(v -> Objects.equals(e, v)))
        .map(Constants.ENV::valueOf)
        .orElse(Constants.ENV.PRODUCTION);
  }

  private String getScheme() {
    return Optional.ofNullable(getConfiguration().getServerFactory().getExternalUrl())
        .map(URI::create)
        .map(URI::getScheme)
        .orElse("http");
  }

  private String getHostName() {
    return Optional.ofNullable(getConfiguration().getServerFactory().getExternalUrl())
        .map(URI::create)
        .map(URI::getHost)
        .orElse("localhost");
  }

  private int getApplicationPort() {
    return ((HttpConnectorFactory)
            ((DefaultServerFactory) getConfiguration().getServerFactory())
                .getApplicationConnectors()
                .get(0))
        .getPort();
  }
}
