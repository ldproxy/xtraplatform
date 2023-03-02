/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.read.ListAppender;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.docs.DocFile;
import io.dropwizard.logging.AbstractAppenderFactory;
import io.dropwizard.logging.DefaultLoggingFactory;
import io.dropwizard.logging.LoggingUtil;
import io.dropwizard.logging.async.AsyncAppenderFactory;
import io.dropwizard.logging.filter.LevelFilterFactory;
import io.dropwizard.logging.layout.LayoutFactory;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @langEn # Logging
 *     <p>## Log-Level
 *     <p>The log level for the application is `INFO` by default. Other possible values are `OFF`,
 *     `ERROR` and `WARN`. For debugging it can be set to `DEBUG` for example:
 *     <p><code>
 * ```yaml
 * logging:
 *   level: DEBUG
 * ```
 * </code>
 *     <p>## Log output
 *     <p>By default, application logs are written to `data/log/xtraplatform.log`. Daily log
 *     rotation is enabled and old logs are zipped and kept for a week. The log file or rotation
 *     settings can be changed:
 *     <p><code>
 * ```yaml
 * logging:
 *   appenders:
 *     - type: file
 *       currentLogFilename: /var/log/ldproxy.log
 *       archive: true
 *       archivedLogFilenamePattern: /var/log/ldproxy-%d.zip
 *       archivedFileCount: 30
 *       timeZone: Europe/Berlin
 * ```
 * </code>
 *     <p>
 * @langDe # Logging
 *     <p>## Log-Level
 *     <p>Der Log-Level für die Applikation ist standardmäßig `INFO`. Weitere mögliche Werte sind
 *     `OFF`, `ERROR` und `WARN`. Für die Fehlersuche kann er zum Beispiel auf `DEBUG` gesetzt
 *     werden:
 *     <p><code>
 * ```yaml
 * logging:
 *   level: DEBUG
 * ```
 * </code>
 *     <p>## Log-Ausgabe
 *     <p>Standardmäßig werden Applikations-Logs nach `data/log/xtraplatform.log` geschrieben. Die
 *     tägliche Log-Rotation ist aktiviert und alte Logs werden gezippt und für eine Woche verwahrt.
 *     Die Log-Datei oder die Rotations-Einstellungen können geändert werden:
 *     <p><code>
 * ```yaml
 * logging:
 *   appenders:
 *     - type: file
 *       currentLogFilename: /var/log/ldproxy.log
 *       archive: true
 *       archivedLogFilenamePattern: /var/log/ldproxy-%d.zip
 *       archivedFileCount: 30
 *       timeZone: Europe/Berlin
 * ```
 * </code>
 *     <p>
 */
@DocFile(path = "application", name = "50-logging.md")
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE, defaultImpl = LoggingConfiguration.class)
public class LoggingConfiguration extends DefaultLoggingFactory {

  private boolean showThirdPartyLoggers;
  private boolean apiRequests;
  private boolean apiRequestUsers;
  private boolean apiRequestHeaders;
  private boolean apiRequestBodies;
  private boolean sqlQueries;
  private boolean sqlResults;
  private boolean configDumps;
  private boolean stackTraces;
  private boolean wiring;

  public LoggingConfiguration() {
    super();
    this.showThirdPartyLoggers = false;
    this.apiRequests = false;
    this.apiRequestUsers = false;
    this.apiRequestHeaders = false;
    this.apiRequestBodies = false;
    this.sqlQueries = false;
    this.sqlResults = false;
    this.configDumps = false;
    this.stackTraces = false;
    this.wiring = false;
  }

  public List<ILoggingEvent> configure(
      MetricRegistry metricRegistry, String name, Optional<List<ILoggingEvent>> record) {
    List<ILoggingEvent> buffer = null;

    if (record.isEmpty()) {
      BufferAppenderFactory appenderFactory = new BufferAppenderFactory();
      buffer = appenderFactory.appender.list;
      getAppenders().add(appenderFactory);
    }

    configure(metricRegistry, name);

    if (record.isPresent()) {
      ImmutableList<Appender<ILoggingEvent>> appenders =
          ImmutableList.copyOf(
              LoggingUtil.getLoggerContext().getLogger("ROOT").iteratorForAppenders());

      for (int j = 0; j < appenders.size() - 1; j++) {
        if (!appenders.get(j).getName().endsWith("console-appender")) {
          for (ILoggingEvent iLoggingEvent : record.get()) {
            appenders.get(j).doAppend(iLoggingEvent);
          }
        }
      }
    }

    return Objects.requireNonNullElse(buffer, List.of());
  }

  @Override
  public void configure(MetricRegistry metricRegistry, String name) {
    super.configure(metricRegistry, name);

    LoggingUtil.getLoggerContext().resetTurboFilterList();

    LoggingUtil.getLoggerContext()
        .addTurboFilter(
            new LoggingFilter(
                showThirdPartyLoggers,
                apiRequests,
                apiRequestUsers,
                apiRequestHeaders,
                apiRequestBodies,
                sqlQueries,
                sqlResults,
                configDumps,
                stackTraces,
                wiring));
  }

  @JsonProperty("showThirdPartyLoggers")
  public boolean getThirdPartyLogging() {
    return showThirdPartyLoggers;
  }

  @JsonProperty("showThirdPartyLoggers")
  public void setThirdPartyLogging(boolean showThirdPartyLoggers) {
    this.showThirdPartyLoggers = showThirdPartyLoggers;
  }

  @JsonProperty
  public boolean isApiRequests() {
    return apiRequests;
  }

  @JsonProperty
  public void setApiRequests(boolean apiRequests) {
    this.apiRequests = apiRequests;
  }

  @JsonProperty
  public boolean isApiRequestUsers() {
    return apiRequestUsers;
  }

  @JsonProperty
  public void setApiRequestUsers(boolean apiRequestUsers) {
    this.apiRequestUsers = apiRequestUsers;
  }

  @JsonProperty
  public boolean isApiRequestHeaders() {
    return apiRequestHeaders;
  }

  @JsonProperty
  public void setApiRequestHeaders(boolean apiRequestHeaders) {
    this.apiRequestHeaders = apiRequestHeaders;
  }

  @JsonProperty
  public boolean isApiRequestBodies() {
    return apiRequestBodies;
  }

  @JsonProperty
  public void setApiRequestBodies(boolean apiRequestBodies) {
    this.apiRequestBodies = apiRequestBodies;
  }

  @JsonProperty
  public boolean isSqlQueries() {
    return sqlQueries;
  }

  @JsonProperty
  public void setSqlQueries(boolean sqlQueries) {
    this.sqlQueries = sqlQueries;
  }

  @JsonProperty
  public boolean isSqlResults() {
    return sqlResults;
  }

  @JsonProperty
  public void setSqlResults(boolean sqlResults) {
    this.sqlResults = sqlResults;
  }

  @JsonProperty
  public boolean isConfigDumps() {
    return configDumps;
  }

  @JsonProperty
  public void setConfigDumps(boolean configDumps) {
    this.configDumps = configDumps;
  }

  @JsonProperty
  public boolean isStackTraces() {
    return stackTraces;
  }

  @JsonProperty
  public void setStackTraces(boolean stackTraces) {
    this.stackTraces = stackTraces;
  }

  @JsonProperty
  public boolean isWiring() {
    return wiring;
  }

  @JsonProperty
  public void setWiring(boolean wiring) {
    this.wiring = wiring;
  }

  @JsonProperty("type")
  public void setType(String type) {}

  private static class BufferAppenderFactory extends AbstractAppenderFactory<ILoggingEvent> {
    private final ListAppender<ILoggingEvent> appender;

    public BufferAppenderFactory() {
      super();

      this.appender = new ListAppender<>();
      appender.setName("buffer");
    }

    @Override
    public Appender<ILoggingEvent> build(
        LoggerContext loggerContext,
        String s,
        LayoutFactory<ILoggingEvent> layoutFactory,
        LevelFilterFactory<ILoggingEvent> levelFilterFactory,
        AsyncAppenderFactory<ILoggingEvent> asyncAppenderFactory) {
      // LOGGER.info("APPENDER ADD {} {}", appender.getName(), appender.getClass());
      appender.start();
      // return wrapAsync(appender, asyncAppenderFactory, loggerContext);
      return appender;
    }
  }
}
