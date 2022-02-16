/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.LogContext;
import java.io.IOException;
import java.io.OutputStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.StreamingOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author zahnen */
@Singleton
@AutoBind
public class LoggingContextCloser implements ContainerResponseFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingContextCloser.class);
  private static final String REQUEST_ID = "X-Request-Id";

  @Inject
  public LoggingContextCloser() {
  }

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {
    if (LogContext.has(LogContext.CONTEXT.REQUEST)) {
      responseContext
          .getHeaders()
          .putSingle(REQUEST_ID, LogContext.get(LogContext.CONTEXT.REQUEST));

      if (responseContext.getEntity() instanceof StreamingOutput
          && !(responseContext.getEntityStream() instanceof OutputStreamCloseListener)) {
        responseContext.setEntityStream(new OutputStreamCloseListener(responseContext));
      } else {
        closeLoggingContext(responseContext);
      }
    }
  }

  private void closeLoggingContext(ContainerResponseContext responseContext) {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug(
          "Sending response: {} {}", responseContext.getStatus(), responseContext.getStatusInfo());

    LogContext.remove(LogContext.CONTEXT.REQUEST);
  }

  private class OutputStreamCloseListener extends OutputStream {
    private final ContainerResponseContext responseContext;
    private final OutputStream entityStream;

    public OutputStreamCloseListener(ContainerResponseContext responseContext) {
      this.responseContext = responseContext;
      this.entityStream = responseContext.getEntityStream();
    }

    @Override
    public void write(int i) throws IOException {
      entityStream.write(i);
    }

    @Override
    public void write(byte[] b) throws IOException {
      entityStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      entityStream.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
      entityStream.flush();
    }

    @Override
    public void close() throws IOException {
      closeLoggingContext(responseContext);

      entityStream.close();
    }
  }
}
