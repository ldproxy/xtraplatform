/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ops.app;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.LoggingFilter;
import de.ii.xtraplatform.ops.domain.OpsEndpoint;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
@Path("logs")
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

  private PatternLayoutEncoder getPatternLayoutEncoder() {
    PatternLayoutEncoder ple = new PatternLayoutEncoder();
    ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
    ple.setContext(loggerContext);
    ple.start();
    return ple;
  }

  @Singleton
  @GET
  @Path("attach")
  @Produces("text/event-stream")
  public void attach(
      @Context SseEventSink sseEventSink,
      @Context Sse sse,
      @QueryParam("logLevel") String logLevel,
      @QueryParam("filters") String filters) {

    if (sseEventSink == null || sse == null || logLevel == null) {
      System.err.println("Error: SseEventSink, Sse or logLevel is null");
      return;
    }

    Map<String, Boolean> flags = new HashMap<>();

    flags.put("showThirdPartyLoggers", false);
    flags.put("apiRequests", false);
    flags.put("apiRequestUsers", false);
    flags.put("apiRequestHeaders", false);
    flags.put("apiRequestBodies", false);
    flags.put("sqlQueries", false);
    flags.put("sqlResults", false);
    flags.put("s3", false);
    flags.put("configDumps", false);
    flags.put("stackTraces", false);
    flags.put("wiring", false);
    flags.put("jobs", false);

    if (filters != null && !filters.isEmpty()) {
      String[] filterKeys = filters.split(",");
      for (String key : filterKeys) {
        if (flags.containsKey(key)) {
          flags.put(key, true);
        } else {
          throw new IllegalArgumentException("Unknown filter: " + key);
        }
      }
    }

    System.out.println("Parsed flags: " + flags);

    ThresholdFilter thresholdFilter = new ThresholdFilter();

    thresholdFilter.setLevel(logLevel);
    thresholdFilter.start();

    CustomLoggingFilter customFilter = new CustomLoggingFilter(flags, loggerContext);
    customFilter.applyFilters();

    CustomOutputStreamAppender appender = getCustomOutputStreamAppender(sseEventSink, sse);
    appender.setContext(loggerContext);

    loggerContext
        .getTurboFilterList()
        .forEach(
            turboFilter -> {
              if (turboFilter instanceof LoggingFilter) {
                LoggingFilter loggingFilter = (LoggingFilter) turboFilter;
                System.out.println("Current LoggingFilter settings:");
                System.out.println("apiRequests: " + loggingFilter.isApiRequests());
                System.out.println("apiRequestUsers: " + loggingFilter.isApiRequestUsers());
                System.out.println("apiRequestHeaders: " + loggingFilter.isApiRequestHeaders());
                System.out.println("apiRequestBodies: " + loggingFilter.isApiRequestBodies());
                System.out.println("sqlQueries: " + loggingFilter.isSqlQueries());
                System.out.println("sqlResults: " + loggingFilter.isSqlResults());
                System.out.println("s3: " + loggingFilter.isS3());
                System.out.println("configDumps: " + loggingFilter.isConfigDumps());
                System.out.println("stackTraces: " + loggingFilter.isStackTraces());
                System.out.println("wiring: " + loggingFilter.isWiring());
                System.out.println("jobs: " + loggingFilter.isJobs());
                System.out.println(
                    "showThirdPartyLoggers: " + loggingFilter.isShowThirdPartyLoggers());
              }
            });

    appender.setName("SSEAppender");
    appender.setEncoder(getPatternLayoutEncoder());
    appender.clearAllFilters();
    appender.addFilter(thresholdFilter);

    appender.start();

    ch.qos.logback.classic.Logger logbackLogger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("ROOT");
    logbackLogger.detachAppender("SSEAppender");
    logbackLogger.addAppender(appender);

    Timer timer = new Timer();
    timer.scheduleAtFixedRate(
        new TimerTask() {
          int lastEventId = 0;

          @Override
          public void run() {
            try {
              if (sseEventSink.isClosed()) {
                timer.cancel();
                logbackLogger.detachAppender(appender);
                appender.stop();
                return;
              }
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

    try {
      if (sseEventSink.isClosed()) {
        logbackLogger.detachAppender(appender);
        appender.stop();
        return;
      }
      OutboundSseEvent event =
          sse.newEventBuilder().name("open").data("Connection established").build();
      sseEventSink.send(event);
    } catch (Exception e) {
      sseEventSink.close();
      System.err.println("Error sending initial event: " + e.getMessage());
    }
  }

  private static CustomOutputStreamAppender getCustomOutputStreamAppender(
      SseEventSink sseEventSink, Sse sse) {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.encoder.PatternLayoutEncoder ple =
        new ch.qos.logback.classic.encoder.PatternLayoutEncoder();
    ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
    ple.setContext(lc);
    ple.start();

    CustomOutputStreamAppender appender = new CustomOutputStreamAppender();
    appender.setEncoder(ple);
    appender.setContext(lc);
    appender.setOutputStream(new SseOutputStream(sseEventSink, sse));
    appender.start();
    return appender;
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
