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
import com.fasterxml.jackson.annotation.JsonProperty;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import java.util.Objects;
import org.slf4j.Marker;

/**
 * @author zahnen
 */
public class LoggingFilter extends TurboFilter {

  private boolean showThirdPartyLoggers;
  private boolean apiRequests;
  private boolean apiRequestUsers;
  private boolean apiRequestHeaders;
  private boolean apiRequestBodies;
  private boolean s3;
  private boolean sqlQueries;
  private boolean sqlResults;
  private boolean configDumps;
  private boolean stackTraces;
  private boolean wiring;

  public LoggingFilter(
      boolean showThirdPartyLoggers,
      boolean apiRequests,
      boolean apiRequestUsers,
      boolean apiRequestHeaders,
      boolean apiRequestBodies,
      boolean sqlQueries,
      boolean sqlResults,
      boolean s3,
      boolean configDumps,
      boolean stackTraces,
      boolean wiring) {
    this.showThirdPartyLoggers = showThirdPartyLoggers;
    this.apiRequests = apiRequests;
    this.apiRequestUsers = apiRequestUsers;
    this.apiRequestHeaders = apiRequestHeaders;
    this.apiRequestBodies = apiRequestBodies;
    this.sqlQueries = sqlQueries;
    this.sqlResults = sqlResults;
    this.s3 = s3;
    this.configDumps = configDumps;
    this.stackTraces = stackTraces;
    this.wiring = wiring;
  }

  @Override
  public FilterReply decide(
      Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {

    if (apiRequests && Objects.equals(marker, MARKER.REQUEST)) {
      return FilterReply.ACCEPT;
    }
    if (apiRequestUsers && Objects.equals(marker, MARKER.REQUEST_USER)) {
      return FilterReply.ACCEPT;
    }
    if (apiRequestHeaders && Objects.equals(marker, MARKER.REQUEST_HEADER)) {
      return FilterReply.ACCEPT;
    }
    if (apiRequestBodies && Objects.equals(marker, MARKER.REQUEST_BODY)) {
      return FilterReply.ACCEPT;
    }

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

    if (s3 && Objects.equals(marker, MARKER.S3)) {
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

  public boolean isS3() {
    return s3;
  }

  public void setS3(boolean s3) {
    this.s3 = s3;
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
