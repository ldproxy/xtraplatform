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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
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
    included = appContext.getConfiguration().getAuditLog().getHttpStatus().getIncluded();
    excluded = appContext.getConfiguration().getAuditLog().getHttpStatus().getExcluded();
  }

  private boolean sufficientHttpCode(int statusCodeInt) {
    String statusCode = String.valueOf(statusCodeInt);

    return !excluded.contains("*")
        && !excluded.contains(statusCode)
        && (included.contains("*") || included.contains(statusCode));
  }

  private void waitForEntity(ContainerResponseContext responseContext) throws IOException {
    if (responseContext.getEntity() instanceof StreamingOutput streamingOutput) {
      // Block until stream is finished
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      streamingOutput.write(baos);

      // Manually set entity
      byte[] buffered = baos.toByteArray();
      responseContext.setEntity(buffered);
      responseContext.getHeaders().putSingle("Content-Length", buffered.length);
    }
  }

  @SuppressWarnings("PMD.CyclomaticComplexity")
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

    try {
      // Wait for the pipeline to finish (sync mode).
      waitForEntity(responseContext);
    } catch (IOException e) {
      // Abort log and request on error.
      auditLog.abortLog(requestId);
      responseContext.setStatus(500);
      responseContext.setEntity(null);
      responseContext.getHeaders().clear();
      throw new ServerErrorException("Internal Server Error", 500, e);
    }

    // Abort log and return if HTTP-Code is not applicable according to the global config
    if (!sufficientHttpCode(responseContext.getStatus())) {
      auditLog.abortLog(requestId);
      return;
    }

    // Log status
    auditLog.setOperationStatus(requestId, Integer.toString(responseContext.getStatus()));

    // Write the final log and save result
    boolean logSuccessful = auditLog.removeAndWriteLog(requestId);

    // Abort request if writing the log was not successful!
    if (!logSuccessful) {
      responseContext.setStatus(500);
      responseContext.setEntity(null);
      responseContext.getHeaders().clear();
      throw new ServerErrorException("Internal Server Error", 500);
    }
  }
}
