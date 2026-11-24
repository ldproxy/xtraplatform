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
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@AutoBind
public class AuditLogResponseFilter implements ContainerResponseFilter {

  private static final Set<Integer> ALL_STATUS_CODES =
      Arrays.stream(Response.Status.values())
          .map(Response.Status::getStatusCode)
          .collect(Collectors.toSet());

  private final AuditLog auditLog;
  private final Set<Integer> included;
  private final Set<Integer> excluded;

  @Inject
  public AuditLogResponseFilter(AuditLog auditLog, AppContext appContext) {
    this.auditLog = auditLog;
    this.included =
        parseStatusCodes(appContext.getConfiguration().getAuditLog().getHttpStatus().getIncluded());
    this.excluded =
        parseStatusCodes(appContext.getConfiguration().getAuditLog().getHttpStatus().getExcluded());
  }

  private boolean isStatusCodeIncluded(int statusCode) {
    return !excluded.contains(statusCode) && included.contains(statusCode);
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
          throwable -> {
            int preStatusCode = responseContext.getStatus();
            int postStatusCode = getStatusCode(responseContext.getStatus(), throwable);
            writeLogEntry(requestId, preStatusCode, postStatusCode);
          });
      return;
    }

    writeLogEntry(requestId, responseContext.getStatus());
  }

  private void writeLogEntry(String requestId, int preStatusCode, int postStatusCode) {
    // Abort log and return if both HTTP-Codes are not included according to the global config
    if (!isStatusCodeIncluded(preStatusCode) && !isStatusCodeIncluded(postStatusCode)) {
      auditLog.abortLog(requestId);
      return;
    }

    // Log postStatusCode if included, preStatusCode else
    int statusCode = isStatusCodeIncluded(postStatusCode) ? postStatusCode : preStatusCode;
    auditLog.setOperationStatus(requestId, Integer.toString(statusCode));

    writeLogEntry(requestId);
  }

  private void writeLogEntry(String requestId, int statusCode) {
    // Abort log and return if HTTP-Code is not included according to the global config
    if (!isStatusCodeIncluded(statusCode)) {
      auditLog.abortLog(requestId);
      return;
    }

    // Log status
    auditLog.setOperationStatus(requestId, Integer.toString(statusCode));

    writeLogEntry(requestId);
  }

  private void writeLogEntry(String requestId) {
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
  // case, so we log if either the returned status code or the status code that should have been
  // returned are included.
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

  private static Set<Integer> parseStatusCodes(List<String> codes) {
    if (codes.contains("*")) {
      return ALL_STATUS_CODES;
    }
    Set<Integer> statusCodes = new HashSet<>();

    for (String code : codes) {
      try {
        if (code.endsWith("xx") && code.length() == 3) {
          int prefix = Integer.parseInt(code.substring(0, 1));
          statusCodes.addAll(
              ALL_STATUS_CODES.stream().filter(c -> c / 100 == prefix).collect(Collectors.toSet()));
        } else {
          int parsedCode = Integer.parseInt(code);
          if (ALL_STATUS_CODES.contains(parsedCode)) {
            statusCodes.add(parsedCode);
          }
        }
      } catch (NumberFormatException e) {
        // ignore invalid codes
      }
    }

    return statusCodes;
  }
}
