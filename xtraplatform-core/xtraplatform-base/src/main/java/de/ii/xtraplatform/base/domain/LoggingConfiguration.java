/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.logging.DefaultLoggingFactory;
import io.dropwizard.logging.LoggingUtil;

/** @author zahnen */
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE, defaultImpl = LoggingConfiguration.class)
public class LoggingConfiguration extends DefaultLoggingFactory {

  private boolean showThirdPartyLoggers;
  private boolean sqlQueries;
  private boolean sqlResults;
  private boolean configDumps;
  private boolean stackTraces;
  private boolean wiring;

  public LoggingConfiguration() {
    super();
    this.showThirdPartyLoggers = false;
    this.sqlQueries = false;
    this.sqlResults = false;
    this.configDumps = false;
    this.stackTraces = false;
    this.wiring = false;
  }

  @Override
  public void configure(MetricRegistry metricRegistry, String name) {
    super.configure(metricRegistry, name);

    LoggingUtil.getLoggerContext().resetTurboFilterList();

    LoggingUtil.getLoggerContext()
        .addTurboFilter(
            new LoggingFilter(
                showThirdPartyLoggers, sqlQueries, sqlResults, configDumps, stackTraces, wiring));
  }

  @JsonProperty("showThirdPartyLoggers")
  public boolean getThirdPartyLogging() {
    return showThirdPartyLoggers;
  }

  @JsonProperty("showThirdPartyLoggers")
  public void setThirdPartyLogging(boolean showThirdPartyLoggers) {
    this.showThirdPartyLoggers = showThirdPartyLoggers;
  }

  public boolean isSqlQueries() {
    return sqlQueries;
  }

  public void setSqlQueries(boolean sqlQueries) {
    this.sqlQueries = sqlQueries;
  }

  public boolean isSqlResults() {
    return sqlResults;
  }

  public void setSqlResults(boolean sqlResults) {
    this.sqlResults = sqlResults;
  }

  public boolean isConfigDumps() {
    return configDumps;
  }

  public void setConfigDumps(boolean configDumps) {
    this.configDumps = configDumps;
  }

  public boolean isStackTraces() {
    return stackTraces;
  }

  public void setStackTraces(boolean stackTraces) {
    this.stackTraces = stackTraces;
  }

  public boolean isWiring() {
    return wiring;
  }

  public void setWiring(boolean wiring) {
    this.wiring = wiring;
  }

  @JsonProperty("type")
  public void setType(String type) {}
}
