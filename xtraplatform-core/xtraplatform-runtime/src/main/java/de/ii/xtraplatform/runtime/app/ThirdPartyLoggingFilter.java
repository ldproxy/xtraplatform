/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.runtime.app;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import de.ii.xtraplatform.runtime.domain.LogContext.MARKER;
import java.util.Objects;
import org.slf4j.Marker;

/**
 * @author zahnen
 */
public class ThirdPartyLoggingFilter extends TurboFilter {

  private final boolean showThirdPartyLoggers;
  private final boolean sqlQueries;
  private final boolean sqlResults;
  private final boolean configDumps;
  private final boolean stackTraces;

  public ThirdPartyLoggingFilter(boolean showThirdPartyLoggers, boolean sqlQueries,
      boolean sqlResults, boolean configDumps, boolean stackTraces) {
    this.showThirdPartyLoggers = showThirdPartyLoggers;
    this.sqlQueries = sqlQueries;
    this.sqlResults = sqlResults;
    this.configDumps = configDumps;
    this.stackTraces = stackTraces;
  }

  @Override
  public FilterReply decide(
      Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {

    if (sqlQueries && (Objects.equals(marker, MARKER.SQL) || logger.getName()
        .equals("slick.jdbc.JdbcBackend.benchmark"))) {
      return FilterReply.ACCEPT;
    }

    if (sqlResults && (Objects.equals(marker, MARKER.SQL_RESULT) || logger.getName()
        .equals("slick.jdbc.StatementInvoker.result"))) {
      return FilterReply.ACCEPT;
    }

    if (configDumps && Objects.equals(marker, MARKER.DUMP)) {
      return FilterReply.ACCEPT;
    }

    if (stackTraces && Objects.equals(marker, MARKER.STACKTRACE)) {
      return FilterReply.ACCEPT;
    }

    if (showThirdPartyLoggers || logger.getName().startsWith("de.ii")) {
      return FilterReply.NEUTRAL;
    }

    return FilterReply.DENY;
  }
}
