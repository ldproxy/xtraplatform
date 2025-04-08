/*
 * Copyright 2025 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ops.app;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;
import org.slf4j.LoggerFactory;

public class CustomOutputStreamAppenderUtil {

  public static CustomOutputStreamAppender getCustomOutputStreamAppender(
      SseEventSink sseEventSink, Sse sse) {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    PatternLayoutEncoder ple = new PatternLayoutEncoder();
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
}
