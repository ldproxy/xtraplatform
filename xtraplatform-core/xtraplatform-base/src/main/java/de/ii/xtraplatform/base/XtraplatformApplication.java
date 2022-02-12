/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base;

import static de.ii.xtraplatform.base.domain.Constants.TMP_DIR_PROP;

import com.google.common.io.ByteSource;
import de.ii.xtraplatform.base.domain.ConfigurationReader;
import de.ii.xtraplatform.base.domain.Constants;
import de.ii.xtraplatform.base.domain.Lifecycle;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.Modules;
import java.io.IOException;
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
public class XtraplatformApplication {
  private static final Logger LOGGER = LoggerFactory.getLogger(XtraplatformApplication.class);

  private static final String ENV_VAR = "XTRAPLATFORM_ENV";
  private static final String DATA_DIR_NAME = "data";
  private static final String BUNDLES_DIR_NAME = "bundles";
  private static final String TMP_DIR_NAME = "tmp";
  private static final String FELIX_CACHE_DIR_NAME = "tmp/felix";

  private final String name;
  private final String version;
  private final Modules modules;

  public XtraplatformApplication(String name, String version, Modules modules) {
    this.name = name;
    this.version = version;
    this.modules = modules;
  }

  public void init(String[] args, List<ByteSource> baseConfigs) {
    Path dataDir =
        getDataDir(args).orElseThrow(() -> new IllegalArgumentException("No data directory found"));
    System.setProperty(TMP_DIR_PROP, dataDir.resolve(TMP_DIR_NAME).toAbsolutePath().toString());

    Constants.ENV env = parseEnvironment();
    ConfigurationReader configurationReader = new ConfigurationReader(baseConfigs);
    Path configurationFile = configurationReader.getConfigurationFile(dataDir, env);

    configurationReader.loadMergedLogging(configurationFile, env);

    LOGGER.info("--------------------------------------------------");
    LOGGER.info("Starting {} {}", name, version);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Data directory: {}", dataDir);
      LOGGER.debug("Environment: {}", env);
      LOGGER.debug("Base configurations: {}", baseConfigs);
    }

    if (LOGGER.isDebugEnabled(LogContext.MARKER.DUMP)) {
      try {
        String cfg = configurationReader.loadMergedConfig(configurationFile, env);
        LOGGER.debug(LogContext.MARKER.DUMP, "Application configuration: \n{}", cfg);
      } catch (IOException e) {
        // ignore
      }
    }
  }

  public void start() {
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

  public void stop() {
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
}
