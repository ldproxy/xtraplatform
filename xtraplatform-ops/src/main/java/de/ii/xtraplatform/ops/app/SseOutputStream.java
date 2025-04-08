/*
 * Copyright 2025 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ops.app;

import java.io.OutputStream;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

public class SseOutputStream extends OutputStream {
  private final SseEventSink sseEventSink;
  private final Sse sse;

  public SseOutputStream(SseEventSink sseEventSink, Sse sse) {
    this.sseEventSink = sseEventSink;
    this.sse = sse;
  }

  @Override
  public void write(int b) {
    // Not used
  }

  @Override
  public void write(byte[] b, int off, int len) {
    String message = new String(b, off, len);
    OutboundSseEvent sseEvent = sse.newEventBuilder().name("log").data(message).build();
    sseEventSink.send(sseEvent);
  }
}
