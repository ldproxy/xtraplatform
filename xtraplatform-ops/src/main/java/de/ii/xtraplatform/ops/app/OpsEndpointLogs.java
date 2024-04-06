/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ops.app;

import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.LoggingFilter;
import de.ii.xtraplatform.ops.domain.OpsEndpoint;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.core.Response;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class OpsEndpointLogs implements OpsEndpoint {

  private final ObjectMapper objectMapper;
  private final LoggerContext loggerContext;

  @Inject
  public OpsEndpointLogs(Jackson jackson) {
    this.objectMapper = jackson.getDefaultObjectMapper();
    this.loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
  }

  @Override
  public Optional<String> getLabel() {
    return Optional.of("Logging");
  }

  @Override
  public String getEntrypoint() {
    return "logs";
  }

  @GET
  public Response getLogs() throws JsonProcessingException {
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

    return Response.ok(
            objectMapper.writeValueAsString(getLogInfo(level, optionalThirdPartyLoggingFilter)))
        .build();
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
                    "apiRequests", loggingFilter.isApiRequests(),
                    "apiRequestUsers", loggingFilter.isApiRequestUsers(),
                    "apiRequestHeaders", loggingFilter.isApiRequestHeaders(),
                    "apiRequestBodies", loggingFilter.isApiRequestBodies(),
                    "sqlQueries", loggingFilter.isSqlQueries(),
                    "sqlResults", loggingFilter.isSqlResults(),
                    "s3", loggingFilter.isS3(),
                    "configDumps", loggingFilter.isConfigDumps(),
                    "stackTraces", loggingFilter.isStackTraces()))
        .orElse(ImmutableMap.of());
  }
}
