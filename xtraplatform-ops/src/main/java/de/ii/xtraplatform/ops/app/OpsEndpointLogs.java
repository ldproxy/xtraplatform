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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
@Path("logs")
public class OpsEndpointLogs implements OpsEndpoint {

  private final ObjectMapper objectMapper;
  private final LoggerContext loggerContext;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private SseBroadcaster broadcaster;

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

  @Singleton
  @GET
  @Path("attach")
  @Produces("text/event-stream")
  public void attach(@Context SseEventSink sseEventSink, @Context Sse sse) {
    if (sseEventSink == null || sse == null) {
      System.err.println("Error: SseEventSink or Sse is null");
      return;
    }

    Timer timer = new Timer();
    timer.scheduleAtFixedRate(
        new TimerTask() {
          int lastEventId = 0;

          @Override
          public void run() {
            try {
              OutboundSseEvent sseEvent =
                  sse.newEventBuilder()
                      .name("message")
                      .id(String.valueOf(lastEventId))
                      .mediaType(MediaType.TEXT_PLAIN_TYPE)
                      .data(String.class, "This is a test message")
                      .reconnectDelay(3000)
                      .comment("test message")
                      .build();
              sseEventSink.send(sseEvent);
              lastEventId++;
            } catch (Exception e) {
              System.err.println("Error in TimerTask: " + e.getMessage());
              timer.cancel();
              sseEventSink.close();
            }
          }
        },
        0,
        5000);
  }

  @POST
  @Path("setLogLevel")
  @Produces(MediaType.APPLICATION_JSON)
  public Response setLogLevel(@QueryParam("logLevel") String logLevel) {
    if (logLevel == null || logLevel.isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("{\"error\":\"logLevel parameter is missing\"}")
          .build();
    }

    return Response.ok("{\"logLevel\":\"" + logLevel + "\"}").build();
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
