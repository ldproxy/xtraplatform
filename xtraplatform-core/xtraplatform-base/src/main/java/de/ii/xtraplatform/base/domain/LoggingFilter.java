/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import java.util.Objects;
import org.slf4j.Marker;

/** @author zahnen */
public class LoggingFilter extends TurboFilter {

  private boolean showThirdPartyLoggers;
  private boolean sqlQueries;
  private boolean sqlResults;
  private boolean configDumps;
  private boolean stackTraces;
  private boolean wiring;

  public LoggingFilter(
      boolean showThirdPartyLoggers,
      boolean sqlQueries,
      boolean sqlResults,
      boolean configDumps,
      boolean stackTraces,
      boolean wiring) {
    this.showThirdPartyLoggers = showThirdPartyLoggers;
    this.sqlQueries = sqlQueries;
    this.sqlResults = sqlResults;
    this.configDumps = configDumps;
    this.stackTraces = stackTraces;
    this.wiring = wiring;
  }

  @Override
  public FilterReply decide(
      Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {

    if (sqlQueries
        && (Objects.equals(marker, MARKER.SQL)
            || logger.getName().equals("slick.jdbc.JdbcBackend.benchmark"))) {
      return FilterReply.ACCEPT;
    }

    if (sqlResults
        && (Objects.equals(marker, MARKER.SQL_RESULT)
            || logger.getName().equals("slick.jdbc.StatementInvoker.result"))) {
      return FilterReply.ACCEPT;
    }

    if (configDumps && Objects.equals(marker, MARKER.DUMP)) {
      return FilterReply.ACCEPT;
    }

    if (stackTraces && Objects.equals(marker, MARKER.STACKTRACE)) {
      return FilterReply.ACCEPT;
    }

    if (wiring && Objects.equals(marker, MARKER.DI)) {
      return FilterReply.ACCEPT;
    }

    if (Objects.isNull(marker) && (showThirdPartyLoggers || logger.getName().startsWith("de.ii"))) {
      return FilterReply.NEUTRAL;
    }

    return FilterReply.DENY;
  }

  public boolean isShowThirdPartyLoggers() {
    return showThirdPartyLoggers;
  }

  public void setShowThirdPartyLoggers(boolean showThirdPartyLoggers) {
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
}
