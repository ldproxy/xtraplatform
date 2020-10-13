/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.runtime;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Joiner;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import de.ii.xtraplatform.runtime.domain.ConfigurationReader;
import de.ii.xtraplatform.runtime.domain.Constants;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.logging.DefaultLoggingFactory;
import io.dropwizard.util.Duration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.main.AutoProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author zahnen */
public class FelixRuntime {
  private static final Logger LOGGER = LoggerFactory.getLogger(FelixRuntime.class);

  private static final String ENV_VAR = "XTRAPLATFORM_ENV";
  private static final String DATA_DIR_NAME = "data";
  private static final String BUNDLES_DIR_NAME = "bundles";
  private static final String FELIX_CACHE_DIR_NAME = "tmp/felix";

  private final String name;
  private final String version;
  private Felix felix;

  public FelixRuntime(String name, String version) {
    this.name = name;
    this.version = version;
  }

  public void init(
      String[] args,
      List<List<String>> bundles,
      List<List<String>> devBundles,
      List<ByteSource> baseConfigs) {
    Map<String, String> felixConfig = new HashMap<>();
    Path dataDir =
        getDataDir(args).orElseThrow(() -> new IllegalArgumentException("No data directory found"));
    Path bundlesDir =
        getBundlesDir(args)
            .orElseThrow(() -> new IllegalArgumentException("No bundles directory found"));
    Constants.ENV env = parseEnvironment();
    ConfigurationReader configurationReader = new ConfigurationReader(baseConfigs);
    Path configurationFile = configurationReader.getConfigurationFile(dataDir, env);

    configurationReader.loadMergedLogging(configurationFile, env);

    LOGGER.info("--------------------------------------------------");
    LOGGER.info("Starting {} {}", name, version);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Data directory: {}", dataDir);
      LOGGER.debug("Bundles directory: {}", bundlesDir);
      LOGGER.debug("Environment: {}", env);
    }

    // trace
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Base configs: {}", baseConfigs);
      try {
        String cfg = configurationReader.loadMergedConfig(configurationFile, env);
        LOGGER.debug("Application configuration: {}", cfg);
      } catch (IOException e) {
        // ignore
      }
    }

    String bundlePrefix =
        "reference:file:" + bundlesDir.toAbsolutePath().toString().replaceAll(" ", "%20") + "/";
    int startLevel = 1;

    for (List<String> level : bundles) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Level {} Bundles: {}", startLevel, level);
      }

      String levelBundles =
          level.stream().map(bundle -> bundlePrefix + bundle).collect(Collectors.joining(" "));

      felixConfig.put(AutoProcessor.AUTO_START_PROP + "." + startLevel, levelBundles);

      startLevel++;
    }

    if (env == Constants.ENV.DEVELOPMENT) {
      for (List<String> level : devBundles) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Level {} Bundles: {}", startLevel, level);
        }

        String levelBundles =
            level.stream().map(bundle -> bundlePrefix + bundle).collect(Collectors.joining(" "));

        felixConfig.put(AutoProcessor.AUTO_START_PROP + "." + startLevel, levelBundles);

        startLevel++;
      }
    }

    felixConfig.put(FelixConstants.FRAMEWORK_BEGINNING_STARTLEVEL, Integer.toString(startLevel));

    felixConfig.put(
        FelixConstants.FRAMEWORK_STORAGE,
        dataDir.resolve(FELIX_CACHE_DIR_NAME).toAbsolutePath().toString());
    felixConfig.put(
        FelixConstants.FRAMEWORK_STORAGE_CLEAN, FelixConstants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
    // Export the host provided service interface package.
    felixConfig.put(
        FelixConstants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
        Joiner.on(',').withKeyValueSeparator(";version=").join(Exports.EXPORTS));
    felixConfig.put(FelixConstants.FRAMEWORK_BOOTDELEGATION, "sun.misc");

    felixConfig.put(Constants.DATA_DIR_KEY, dataDir.toAbsolutePath().toString());
    felixConfig.put(Constants.ENV_KEY, env.name());
    felixConfig.put(Constants.USER_CONFIG_PATH_KEY, configurationFile.toAbsolutePath().toString());

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Felix config: {}", felixConfig);
    }

    try {
      this.felix = new Felix(felixConfig);
      felix.init();
    } catch (Exception ex) {
      LOGGER.error("Could not initialize felix: {}", ex.getMessage());
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("", ex);
      }
    }

    AutoProcessor.process(felixConfig, felix.getBundleContext());
  }

  public void start() {
    try {
      felix.start();

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Felix started");
      }
    } catch (Exception ex) {
      LOGGER.error("Could not start felix: {}", ex.getMessage());
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("", ex);
      }
    }
  }

  public void stop(long timeout) {
    LOGGER.info("Shutting down {}", name);

    try {
      felix.stop();
      felix.waitForStop(timeout);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Felix stopped");
      }
    } catch (Exception ex) {
      LOGGER.error("Could not stop felix: {}", ex.getMessage());
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("", ex);
      }
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

  private Optional<Path> getBundlesDir(String[] args) {
    Path bundlesDir;

    if (args.length >= 2) {
      bundlesDir = Paths.get(args[1]);
    } else {
      bundlesDir = Paths.get(BUNDLES_DIR_NAME).toAbsolutePath();
      if (!Files.isDirectory(bundlesDir)) {
        bundlesDir = Paths.get("../" + BUNDLES_DIR_NAME).toAbsolutePath();
      }
    }
    if (!Files.isDirectory(bundlesDir)) {
      return Optional.empty();
    }

    return Optional.of(bundlesDir);
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
