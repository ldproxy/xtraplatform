/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import static de.ii.xtraplatform.base.domain.Constants.TMP_DIR_PROP;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.ii.xtraplatform.base.domain.Constants.ENV;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.base.domain.StoreSource.Content;
import de.ii.xtraplatform.base.domain.StoreSource.Type;
import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
public class AppLauncher implements AppContext {
  private static final Logger LOGGER = LoggerFactory.getLogger(AppLauncher.class);

  private static final String ENV_VAR = "XTRAPLATFORM_ENV";
  private static final String DATA_DIR_NAME = "data";
  private static final String TMP_DIR_NAME = "tmp";

  private final String name;
  private final String version;
  private final Set<CfgStoreDriver> drivers;
  private final ExecutorService startupExecutor;
  private Constants.ENV env;
  private Path dataDir;
  private Path tmpDir;
  private AppConfiguration cfg;
  private URI uri;

  public AppLauncher(String name, String version) {
    this.name = name;
    this.version = version;
    this.drivers = new HashSet<>();
    this.startupExecutor =
        // MoreExecutors.getExitingExecutorService(
        (ThreadPoolExecutor)
            Executors.newCachedThreadPool(
                new ThreadFactoryBuilder().setNameFormat("startup-%d").build()); // );
    ;
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
  public Path getDataDir() {
    return dataDir;
  }

  @Override
  public Path getTmpDir() {
    return tmpDir;
  }

  @Override
  public AppConfiguration getConfiguration() {
    return cfg;
  }

  @Override
  public String getInstanceName() {
    return String.format("%s@%s", name, getRealHostName());
  }

  @Override
  public URI getUri() {
    return uri;
  }

  public String init(String[] args, Map<String, ByteSource> baseConfigs) throws IOException {
    this.dataDir =
        getDataDir(args).orElseThrow(() -> new IllegalArgumentException("No data directory found"));
    this.tmpDir = dataDir.resolve(TMP_DIR_NAME).toAbsolutePath();
    System.setProperty(TMP_DIR_PROP, tmpDir.toString());

    this.env = parseEnvironment();
    ConfigurationReader configurationReader = new ConfigurationReader(baseConfigs);

    List<ILoggingEvent> buffer = configurationReader.loadMergedLogging(Optional.empty(), env);

    LOGGER.info("--------------------------------------------------");
    LOGGER.info("Starting {} v{}", name, version);

    this.cfg = configurationReader.loadMergedConfig(Map.of(), env);

    this.drivers.add(new CfgStoreDriverFs(dataDir));
    this.drivers.add(new CfgStoreDriverHttp(tmpDir, cfg.getHttpClient()));

    ServiceLoader<CfgStoreDriver> services = ServiceLoader.load(CfgStoreDriver.class);
    services.iterator().forEachRemaining(this.drivers::add);

    Map<String, InputStream> cfgs = getCfgs(cfg.getStore().getSources());

    this.cfg = configurationReader.loadMergedConfig(cfgs, env);

    cfg.getLoggingFactory().configure(new MetricRegistry(), "xtraplatform", Optional.of(buffer));

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Data directory: {}", dataDir);
      LOGGER.debug("Environment: {}", env);
      LOGGER.debug("Base configurations: {}", configurationReader.getBaseConfigs(env).keySet());
      LOGGER.debug("User configurations: {}", cfgs.keySet());
    }
    if (LOGGER.isDebugEnabled(LogContext.MARKER.DUMP)) {
      LOGGER.debug(
          LogContext.MARKER.DUMP,
          "Application configuration: \n{}",
          configurationReader.asString(cfg));
    }

    String externalUrl = getConfiguration().getServerFactory().getExternalUrl();
    if (Strings.isNullOrEmpty(externalUrl)) {
      this.uri =
          URI.create(
              String.format("%s://%s:%d/", getScheme(), getHostName(), getApplicationPort()));
    } else {
      this.uri =
          URI.create(
              externalUrl.endsWith("/")
                  ? externalUrl.substring(0, externalUrl.length() - 1)
                  : externalUrl);
    }

    return String.format(
        "%s_%s", cfg.getModules().getMinMaturity(), cfg.getModules().getMinMaintenance());
  }

  public void start(App modules, List<String> skipped) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Startup mode: {}", cfg.getModules().getStartup());
      LOGGER.debug(
          "Module constraints: [minMaturity: {}, minMaintenance: {}]",
          cfg.getModules().getMinMaturity(),
          cfg.getModules().getMinMaintenance());
      if (!skipped.isEmpty()) {
        LOGGER.debug("Skipped modules: {}", skipped);
      }
    }

    modules.lifecycle().get().stream()
        .sorted(Comparator.comparingInt(AppLifeCycle::getPriority))
        .forEach(
            lifecycle -> {
              if (lifecycle.getPriority() == 0
                  || !getConfiguration().getModules().isStartupAsync()) {
                start(lifecycle);
              } else {
                startupExecutor.submit(() -> start(lifecycle));
              }
            });
  }

  private void start(AppLifeCycle lifecycle) {
    if (LOGGER.isDebugEnabled(MARKER.DI)) {
      LOGGER.debug(
          MARKER.DI,
          "Starting {} ({})",
          lifecycle.getClass().getSimpleName(),
          lifecycle.getPriority());
    }
    try {
      lifecycle.onStart(cfg.getModules().isStartupAsync()).toCompletableFuture().get();
    } catch (Throwable e) {
      LogContext.error(
          LOGGER,
          e,
          "Error starting {} ({})",
          lifecycle.getClass().getSimpleName(),
          lifecycle.getPriority());
      return;
    }
    if (LOGGER.isDebugEnabled(MARKER.DI)) {
      LOGGER.debug(
          MARKER.DI,
          "Started {} ({})",
          lifecycle.getClass().getSimpleName(),
          lifecycle.getPriority());
    }
  }

  public void stop(App modules) {
    LOGGER.info("Shutting down {}", name);

    modules.lifecycle().get().stream()
        .sorted(Comparator.comparingInt(AppLifeCycle::getPriority).reversed())
        .forEach(
            lifecycle -> {
              if (LOGGER.isDebugEnabled(MARKER.DI)) {
                LOGGER.debug(
                    MARKER.DI,
                    "Stopping {} ({})",
                    lifecycle.getClass().getSimpleName(),
                    lifecycle.getPriority());
              }
              try {
                lifecycle.onStop();
              } catch (Throwable e) {
                // ignore
              }
            });

    startupExecutor.shutdownNow();

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
        .orElse(Constants.ENV.NATIVE);
  }

  private Map<String, InputStream> getCfgs(List<StoreSource> sources) {
    List<StoreSource> cfgSources = findSources(sources);

    return cfgSources.stream()
        .flatMap(
            source -> {
              Optional<CfgStoreDriver> driver = findDriver(source, true);

              if (driver.isPresent()) {
                try {
                  Optional<InputStream> cfg = driver.get().load(source);

                  if (cfg.isPresent()) {
                    String label =
                        source.getContent() == Content.CFG
                            ? source.getLabel()
                            : source.getLabel() + "/" + CfgStoreDriverFs.CFG_YML;
                    return Stream.of(new SimpleImmutableEntry<>(label, cfg.get()));
                  }
                } catch (Throwable e) {
                  LogContext.error(
                      LOGGER,
                      e,
                      "{} for {} could not be loaded",
                      Content.RESOURCES.getLabel(),
                      source.getLabel());
                }
              }

              return Stream.empty();
            })
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private List<StoreSource> findSources(List<StoreSource> sources) {
    return sources.stream()
        .filter(
            source ->
                !Objects.equals(source.getType(), Type.EMPTY.name())
                    && (source.getContent() == Content.ALL || source.getContent() == Content.CFG))
        .collect(Collectors.toUnmodifiableList());
  }

  private Optional<CfgStoreDriver> findDriver(StoreSource storeSource, boolean warn) {
    final boolean[] foundUnavailable = {false};

    // TODO: driver content types, s3 only supports resources
    Optional<CfgStoreDriver> driver =
        drivers.stream()
            .filter(d -> Objects.equals(d.getType(), storeSource.getType()))
            .filter(
                d -> {
                  if (!d.isAvailable(storeSource)) {
                    if (warn) {
                      LOGGER.warn(
                          "{} for {} is not available.",
                          Content.CFG.getLabel(),
                          storeSource.getLabel());
                    }
                    foundUnavailable[0] = true;
                    return false;
                  }
                  return true;
                })
            .findFirst();

    if (driver.isEmpty() && !foundUnavailable[0]) {
      LOGGER.error("No cfg driver found for source {}.", storeSource.getLabel());
    }

    return driver;
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

  private String getRealHostName() {
    String host = System.getenv("HOSTNAME");

    if (Objects.nonNull(host)) {
      return host;
    }

    try {
      String result = InetAddress.getLocalHost().getHostName();
      if (!Strings.isNullOrEmpty(result)) {
        return result;
      }
    } catch (UnknownHostException e) {
      // failed;  try alternate means.
    }

    return "UNKNOWN";
  }

  private int getApplicationPort() {
    return ((HttpConnectorFactory)
            ((DefaultServerFactory) getConfiguration().getServerFactory())
                .getApplicationConnectors()
                .get(0))
        .getPort();
  }
}
