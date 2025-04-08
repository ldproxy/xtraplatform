/*
 * Copyright 2025 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ops.app;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import java.io.OutputStream;

public class CustomOutputStreamAppender extends OutputStreamAppender<ILoggingEvent> {
  @Override
  public void setOutputStream(OutputStream outputStream) {
    super.setOutputStream(outputStream);
  }

  public void setEncoder(PatternLayoutEncoder ple) {
    super.setEncoder(ple);
  }
}
