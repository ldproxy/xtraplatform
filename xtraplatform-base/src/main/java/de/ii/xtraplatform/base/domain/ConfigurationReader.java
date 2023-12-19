/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import static de.ii.xtraplatform.base.domain.util.JacksonModules.DESERIALIZE_IMMUTABLE_BUILDER_NESTED;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ext.Java7Support;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import de.ii.xtraplatform.base.domain.Constants.ENV;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.logging.common.AbstractAppenderFactory;
import io.dropwizard.logging.common.ConsoleAppenderFactory;
import io.dropwizard.util.DataSize;
import io.dropwizard.util.Duration;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class ConfigurationReader {

  enum APPENDER {
    CONSOLE,
    OTHER
  }

  public static final String CFG_FILE_NAME = "cfg.yml";

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
  private final EnvironmentVariableSubstitutor envSubstitutor;

  // workaround for https://github.com/FasterXML/jackson-databind/issues/4078
  static {
    try {
      Java7Support java7Support = Java7Support.instance();
    } catch (Throwable e) {
      // ignore
    }
  }

  public ConfigurationReader(Map<String, ByteSource> configsToMergeAfterBase) {
    this.configsToMergeAfterBase = configsToMergeAfterBase;

    this.mapper =
        Jackson.newObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID))
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(MapperFeature.AUTO_DETECT_FIELDS)
            .disable(MapperFeature.AUTO_DETECT_GETTERS)
            .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
            .disable(MapperFeature.AUTO_DETECT_SETTERS)
            .registerModule(DESERIALIZE_IMMUTABLE_BUILDER_NESTED)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    this.mergeMapper = getMergeMapper(mapper);

    this.envSubstitutor = new EnvironmentVariableSubstitutor(false);
  }

  public ObjectMapper getMapper() {
    return mapper;
  }

  private String read(ByteSource byteSource) throws IOException {
    String read = byteSource.asCharSource(StandardCharsets.UTF_8).read();
    String replace = envSubstitutor.replace(read);
    return replace;
  }

  public AppConfiguration loadMergedConfig(Map<String, InputStream> userCfgs, Constants.ENV env)
      throws IOException {
    ModifiableAppConfiguration builder =
        mapper.readValue(read(getBaseConfig()), ModifiableAppConfiguration.class);

    for (ByteSource envCfg : getEnvConfigs(env).values()) {
      mergeMapper.readerForUpdating(builder).readValue(read(envCfg));
    }

    // TODO: error message with entry.getKey()
    for (Map.Entry<String, InputStream> userCfg : userCfgs.entrySet()) {
      mergeMapper
          .readerForUpdating(builder)
          .readValue(read(ByteSource.wrap(userCfg.getValue().readAllBytes())));
    }

    applyLogFormat(builder.getLoggingFactory(), env);

    applyForcedDefaults(builder, env);

    return builder.toImmutable();
  }

  public InputStream asInputStream(AppConfiguration cfg) throws IOException {
    return new ByteArrayInputStream(mapper.writeValueAsBytes(cfg));
  }

  public String asString(AppConfiguration cfg) throws IOException {
    return mapper.writeValueAsString(cfg);
  }

  public List<ILoggingEvent> loadMergedLogging(Optional<Path> userCfg, ENV env) {
    LoggingConfiguration loggingFactory;

    try {
      JsonNode jsonNodeBase = mapper.readTree(read(getBaseConfig()));

      loggingFactory =
          mapper.readerFor(LoggingConfiguration.class).readValue(jsonNodeBase.at(LOGGING_CFG_KEY));
      // TODO: System.out.println(env + "  " + getEnvConfigs(env).keySet());
      for (ByteSource envCfg : getEnvConfigs(env).values()) {
        JsonNode jsonNodeMerge = mapper.readTree(read(envCfg));

        mergeMapper.readerForUpdating(loggingFactory).readValue(jsonNodeMerge.at(LOGGING_CFG_KEY));
      }

      if (userCfg.isPresent() && Files.exists(userCfg.get())) {
        JsonNode jsonNodeUser =
            mapper.readTree(read(ByteSource.wrap(Files.readAllBytes(userCfg.get()))));

        mergeMapper.readerForUpdating(loggingFactory).readValue(jsonNodeUser.at(LOGGING_CFG_KEY));
      }
    } catch (Throwable e) {
      // use defaults
      // loggingFactory = new LoggingConfiguration();
      // TODO: defaults lead to error in loggingFactory.configure
      throw new IllegalStateException("Error parsing base and env configs", e);
    }

    applyLogFormat(loggingFactory, env);

    return loggingFactory.configure(new MetricRegistry(), "xtraplatform", Optional.empty());
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
    cfg.getServerFactory().setRegisterDefaultExceptionMappers(false);

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
    mergeMapper.configOverride(Set.class).setMergeable(false);
    mergeMapper.configOverride(List.class).setMergeable(false);
    mergeMapper.configOverride(Map.class).setMergeable(false);
    mergeMapper.configOverride(Duration.class).setMergeable(false);
    mergeMapper.configOverride(DataSize.class).setMergeable(false);

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
