/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.services.domain.AuditLog;
import de.ii.xtraplatform.web.domain.JoinableStreamingOutput;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Singleton
@AutoBind
public class AuditLogResponseFilter implements ContainerResponseFilter {

  private final AuditLog auditLog;
  private final List<String> included;
  private final List<String> excluded;

  @Inject
  public AuditLogResponseFilter(AuditLog auditLog, AppContext appContext) {
    this.auditLog = auditLog;
    this.included = appContext.getConfiguration().getAuditLog().getHttpStatus().getIncluded();
    this.excluded = appContext.getConfiguration().getAuditLog().getHttpStatus().getExcluded();
  }

  private boolean sufficientHttpCode(int statusCodeInt) {
    String statusCode = String.valueOf(statusCodeInt);

    return !excluded.contains("*")
        && !excluded.contains(statusCode)
        && (included.contains("*") || included.contains(statusCode));
  }

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {
    // Return if auditLog is disabled in global config (cfg.yml)
    if (!auditLog.isEnabled()) {
      return;
    }

    // Return if the request context is missing
    if (Objects.isNull(requestContext)) {
      return;
    }

    // Return if the requestId is missing
    if (!(requestContext.getProperty("REQUEST_ID") instanceof String requestId)) {
      return;
    }

    // Return if no log is available (for example if the log was aborted during another step)
    if (!auditLog.logIsAvailable(requestId)) {
      return;
    }

    // Abort log and return if the response context is missing
    if (Objects.isNull(responseContext)) {
      auditLog.abortLog(requestId);
      return;
    }

    if (responseContext.getEntity() instanceof JoinableStreamingOutput streamingOutput) {
      streamingOutput.whenComplete(
          (throwable) -> {
            int statusCode = getStatusCode(responseContext.getStatus(), throwable);

            writeLogEntry(requestId, statusCode);
          });
      return;
    }

    writeLogEntry(requestId, responseContext.getStatus());
  }

  private void writeLogEntry(String requestId, int statusCode) {
    // Abort log and return if HTTP-Code is not applicable according to the global config
    if (!sufficientHttpCode(statusCode)) {
      auditLog.abortLog(requestId);
      return;
    }
    // Log status
    auditLog.setOperationStatus(requestId, Integer.toString(statusCode));

    // Write the final log and save result
    boolean logSuccessful = auditLog.removeAndWriteLog(requestId);

    // Abort request if writing the log was not successful!
    if (!logSuccessful) {
      throw new InternalServerErrorException();
    }
  }

  // This is not one hundred percent correct. If the web server already started writing the response
  // to the client before an exception occurred, the returned status code will always be 200, even
  // if the response is broken and the log shows an error. But there is no way to determine this
  // case, so we log the status code that should have been returned.
  private static int getStatusCode(int initialStatusCode, Throwable throwable) {
    int statusCode = initialStatusCode;

    if (Objects.nonNull(throwable)) {
      if (throwable instanceof WebApplicationException webAppException) {
        statusCode = webAppException.getResponse().getStatus();
      } else {
        statusCode = 500;
      }
    }
    return statusCode;
  }
}
