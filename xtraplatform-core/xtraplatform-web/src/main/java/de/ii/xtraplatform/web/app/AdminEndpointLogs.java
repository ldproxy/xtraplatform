/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.LoggingFilter;
import de.ii.xtraplatform.web.domain.AdminSubEndpoint;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class AdminEndpointLogs implements AdminSubEndpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(AdminEndpointLogs.class);

  private final HttpServlet servlet;
  private final ObjectMapper objectMapper;
  private final LoggerContext loggerContext;

  @Inject
  public AdminEndpointLogs(Jackson jackson) {
    this.objectMapper = jackson.getDefaultObjectMapper();
    this.servlet = new LogsServlet();
    this.loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
  }

  @Override
  public Optional<String> getLabel() {
    return Optional.of("Logging");
  }

  @Override
  public String getPath() {
    return "/logs";
  }

  @Override
  public HttpServlet getServlet() {
    return servlet;
  }

  class LogsServlet extends HttpServlet {
    private static final long serialVersionUID = 3772654177231086757L;
    private static final String CONTENT_TYPE = "application/json";
    private static final String CACHE_CONTROL = "Cache-Control";
    private static final String NO_CACHE = "must-revalidate,no-cache,no-store";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setHeader(CACHE_CONTROL, NO_CACHE);
      resp.setContentType(CONTENT_TYPE);

      String level =
          loggerContext
              .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME)
              .getLevel()
              .toString();

      Optional<LoggingFilter> optionalThirdPartyLoggingFilter =
          loggerContext.getTurboFilterList().stream()
              .filter(turboFilter -> turboFilter instanceof LoggingFilter)
              .map(turboFilter -> (LoggingFilter) turboFilter)
              .findFirst();

      try (PrintWriter writer = resp.getWriter()) {
        objectMapper.writeValue(writer, getLogInfo(level, optionalThirdPartyLoggingFilter));
      }
    }

    private ImmutableMap<String, Object> getLogInfo(
        String level, Optional<LoggingFilter> optionalThirdPartyLoggingFilter) {
      return ImmutableMap.of(
          "level", level, "filter", getFilterInfo(optionalThirdPartyLoggingFilter));
    }

    private ImmutableMap<String, Boolean> getFilterInfo(
        Optional<LoggingFilter> optionalThirdPartyLoggingFilter) {
      return optionalThirdPartyLoggingFilter
          .map(
              loggingFilter ->
                  ImmutableMap.of(
                      "sqlQueries", loggingFilter.isSqlQueries(),
                      "sqlResults", loggingFilter.isSqlResults(),
                      "configDumps", loggingFilter.isConfigDumps(),
                      "stackTraces", loggingFilter.isStackTraces()))
          .orElse(ImmutableMap.of());
    }
  }
}
