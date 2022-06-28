/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.logging.AbstractAppenderFactory;
import io.dropwizard.logging.ConsoleAppenderFactory;
import io.dropwizard.logging.DefaultLoggingFactory;
import io.dropwizard.util.Duration;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class ConfigurationReader {

  enum APPENDER {
    CONSOLE,
    OTHER
  }

  public static final String CFG_FILE_NAME = "cfg.yml";
  public static final String CFG_FILE_NAME_LEGACY = "xtraplatform.json";
  public static final String DEFAULT_VALUE = "_DEFAULT_";

  private static final String CFG_FILE_BASE = "/cfg.base.yml";
  private static final String CFG_FILE_CONSOLE = "/cfg.console.yml";
  private static final String CFG_FILE_LOGFILE = "/cfg.logfile.yml";
  private static final String CFG_FILE_DEV = "/cfg.dev.yml";

  private static final String LOGGING_CFG_KEY = "/logging";
  private static final Map<Constants.ENV, Map<APPENDER, String>> LOG_FORMATS =
      ImmutableMap.of(
          Constants.ENV.DEVELOPMENT,
              ImmutableMap.of(
                  APPENDER.CONSOLE,
                      "%highlight(%-5p) %gray([%d{ISO8601,%dwTimeZone}]) %cyan(%24.-24mdc{SERVICE}) - %m %green(%replace([%mdc{REQUEST}]){'\\[\\]',''}) %gray([%c{44}]) %magenta([%t]) %blue(%marker) %n%rEx",
                  APPENDER.OTHER,
                      "%-5p [%d{ISO8601,%dwTimeZone}] %-24.-24mdc{SERVICE} - %m %replace([%mdc{REQUEST}]){'\\[\\]',''} [%c{44}] [%t] %n%rEx"),
          Constants.ENV.NATIVE,
              ImmutableMap.of(
                  APPENDER.CONSOLE,
                      "%highlight(%-5p) %gray([%d{ISO8601,%dwTimeZone}]) %cyan(%24.-24mdc{SERVICE}) - %m %green(%replace([%mdc{REQUEST}]){'\\[\\]',''}) %n%rEx",
                  APPENDER.OTHER,
                      "%-5p [%d{ISO8601,%dwTimeZone}] %-24.-24mdc{SERVICE} - %m %replace([%mdc{REQUEST}]){'\\[\\]',''} %n%rEx"),
          // TODO: is this needed?
          Constants.ENV.CONTAINER,
              ImmutableMap.of(
                  APPENDER.CONSOLE,
                      "%highlight(%-5p) %gray([%d{ISO8601,%dwTimeZone}]) %cyan(%24.-24mdc{SERVICE}) - %m %green(%replace([%mdc{REQUEST}]){'\\[\\]',''}) %n%rEx",
                  APPENDER.OTHER,
                      "%-5p [%d{ISO8601,%dwTimeZone}] %-24.-24mdc{SERVICE} - %m %replace([%mdc{REQUEST}]){'\\[\\]',''} %n%rEx"));

  private final Map<String, ByteSource> configsToMergeAfterBase;
  private final ObjectMapper mapper;
  private final ObjectMapper mergeMapper;

  public ConfigurationReader(Map<String, ByteSource> configsToMergeAfterBase) {
    this.configsToMergeAfterBase = configsToMergeAfterBase;

    this.mapper =
        Jackson.newObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID))
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(MapperFeature.AUTO_DETECT_FIELDS)
            .disable(MapperFeature.AUTO_DETECT_GETTERS)
            .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
            .disable(MapperFeature.AUTO_DETECT_SETTERS);

    this.mergeMapper = getMergeMapper(mapper);
  }

  public Path getConfigurationFile(Path dataDir, Constants.ENV environment) {
    Path defaultPath = dataDir.resolve(CFG_FILE_NAME).toAbsolutePath();
    Path legacyPath = dataDir.resolve(CFG_FILE_NAME_LEGACY).toAbsolutePath();

    if (Files.exists(defaultPath)) {
      return defaultPath;
    } else if (Files.exists(legacyPath)) {
      return legacyPath;
    } else {
      // TODO
      Optional<ByteSource> configurationFileTemplate =
          getConfigurationFileTemplate(environment.name().toLowerCase());

      if (configurationFileTemplate.isPresent()) {
        try {
          configurationFileTemplate.get().copyTo(new FileOutputStream(defaultPath.toFile()));
        } catch (IOException e) {
          // ignore
        }
      }

      return defaultPath;
    }
  }

  public InputStream loadMergedConfig(Optional<InputStream> userConfig, Constants.ENV env)
      throws IOException {
    AppConfiguration base = mapper.readValue(getBaseConfig().openStream(), AppConfiguration.class);

    for (ByteSource byteSource : getEnvConfigs(env).values()) {
      mergeMapper.readerForUpdating(base).readValue(byteSource.openStream());
    }

    if (userConfig.isPresent()) {
      mergeMapper.readerForUpdating(base).readValue(userConfig.get());
    }

    applyLogFormat(base.getLoggingConfiguration(), env);

    applyForcedDefaults(base, env);

    return new ByteArrayInputStream(mapper.writeValueAsBytes(base));
  }

  public String loadMergedConfigAsString(Path userConfig, Constants.ENV env) throws IOException {
    Optional<InputStream> inputStream =
        userConfig.toFile().exists()
            ? Optional.of(Files.newInputStream(userConfig))
            : Optional.empty();
    String cfg =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return loadMergedConfig(inputStream, env);
          }
        }.asCharSource(Charsets.UTF_8).read();

    EnvironmentVariableSubstitutor environmentVariableSubstitutor =
        new EnvironmentVariableSubstitutor(false);

    return environmentVariableSubstitutor.replace(cfg);
  }

  public AppConfiguration configFromString(String cfg) throws IOException {
    return mapper.readValue(cfg, AppConfiguration.class);
  }

  public void loadMergedLogging(Path userConfig, Constants.ENV env) {
    LoggingConfiguration loggingFactory;

    try {
      JsonNode jsonNodeBase = mapper.readTree(getBaseConfig().openStream());

      loggingFactory =
          mapper.readerFor(LoggingConfiguration.class).readValue(jsonNodeBase.at(LOGGING_CFG_KEY));

      for (ByteSource byteSource : getEnvConfigs(env).values()) {
        JsonNode jsonNodeMerge = mapper.readTree(byteSource.openStream());

        mergeMapper.readerForUpdating(loggingFactory).readValue(jsonNodeMerge.at(LOGGING_CFG_KEY));
      }

      if (userConfig.toFile().exists()) {
        JsonNode jsonNodeUser = mapper.readTree(userConfig.toFile());

        mergeMapper.readerForUpdating(loggingFactory).readValue(jsonNodeUser.at(LOGGING_CFG_KEY));
      }
    } catch (Throwable e) {
      // use defaults
      loggingFactory = new LoggingConfiguration();
    }

    applyLogFormat(loggingFactory, env);

    loggingFactory.configure(new MetricRegistry(), "xtraplatform");
  }

  public Map<String, ByteSource> getBaseConfigs(Constants.ENV env) {
    return Stream.concat(
            Stream.of(
                new SimpleEntry<>(
                    Path.of("/").relativize(Path.of(CFG_FILE_BASE)).toString(), getBaseConfig())),
            getEnvConfigs(env).entrySet().stream())
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Map<String, ByteSource> getEnvConfigs(Constants.ENV env) {
    List<String> envConfigs = new ArrayList<>();
    if (env.isDev() || env.isContainer()) {
      envConfigs.add(CFG_FILE_CONSOLE);
    } else {
      envConfigs.add(CFG_FILE_LOGFILE);
    }
    if (env.isDev()) {
      envConfigs.add(CFG_FILE_DEV);
    }

    return Stream.concat(
            envConfigs.stream()
                .map(
                    cfgPath ->
                        new SimpleEntry<>(
                            Path.of("/").relativize(Path.of(cfgPath)).toString(),
                            Resources.asByteSource(
                                Resources.getResource(ConfigurationReader.class, cfgPath)))),
            configsToMergeAfterBase.entrySet().stream())
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Deprecated
  public void loadLegacyLogging(Path userConfig) {
    ObjectMapper jsonMapper = Jackson.newObjectMapper();

    DefaultLoggingFactory loggingFactory;

    try {
      JsonNode jsonNode = jsonMapper.readTree(userConfig.toFile());

      loggingFactory =
          jsonMapper.readerFor(DefaultLoggingFactory.class).readValue(jsonNode.at(LOGGING_CFG_KEY));
    } catch (Throwable e) {
      // use defaults
      loggingFactory = new DefaultLoggingFactory();
    }

    loggingFactory.configure(new MetricRegistry(), "xtraplatform");
  }

  // TODO: special console pattern
  // TODO: only set format if default is set, so custom format in cfg.yml is possible
  private static void applyLogFormat(LoggingConfiguration loggingConfiguration, Constants.ENV env) {
    loggingConfiguration.getAppenders().stream()
        .filter(
            iLoggingEventAppenderFactory ->
                iLoggingEventAppenderFactory instanceof AbstractAppenderFactory)
        .forEach(
            iLoggingEventAppenderFactory -> {
              AbstractAppenderFactory abstractAppenderFactory =
                  (AbstractAppenderFactory) iLoggingEventAppenderFactory;

              if (LOG_FORMATS.containsKey(env)) {
                if (iLoggingEventAppenderFactory instanceof ConsoleAppenderFactory) {
                  abstractAppenderFactory.setLogFormat(LOG_FORMATS.get(env).get(APPENDER.CONSOLE));
                } else {
                  abstractAppenderFactory.setLogFormat(LOG_FORMATS.get(env).get(APPENDER.OTHER));
                }
              }
            });
  }

  private static void applyForcedDefaults(AppConfiguration cfg, Constants.ENV env) {
    cfg.getServerFactory()
        .getApplicationConnectors()
        .forEach(
            connectorFactory -> {
              if (connectorFactory instanceof HttpConnectorFactory) {
                ((HttpConnectorFactory) connectorFactory).setUseForwardedHeaders(true);
              }
            });
  }

  private static ObjectMapper getMergeMapper(ObjectMapper baseMapper) {
    ObjectMapper mergeMapper = baseMapper.copy().setDefaultMergeable(true);
    mergeMapper.configOverride(List.class).setMergeable(false);
    mergeMapper.configOverride(Map.class).setMergeable(false);
    mergeMapper.configOverride(Duration.class).setMergeable(false);

    return mergeMapper;
  }

  private ByteSource getBaseConfig() {
    return Resources.asByteSource(Resources.getResource(getClass(), CFG_FILE_BASE));
  }

  public Optional<ByteSource> getConfigurationFileTemplate(String environment) {
    return getConfigurationFileTemplateFromClassBundle(environment, ConfigurationReader.class);
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
}
