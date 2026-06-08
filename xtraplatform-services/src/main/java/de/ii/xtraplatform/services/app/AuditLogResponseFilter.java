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
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class AuditLogResponseFilter implements ContainerResponseFilter {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogResponseFilter.class);
  private final AuditLog auditLog;
  private final AppContext appContext;

  @Inject
  public AuditLogResponseFilter(AuditLog auditLog, AppContext appContext) {
    this.auditLog = auditLog;
    this.appContext = appContext;
  }

  private boolean sufficientHttpCode(int statusCodeInt) {
    String statusCode = String.valueOf(statusCodeInt);
    List<String> included =
        appContext.getConfiguration().getAuditLog().getHttpStatus().getIncluded();
    List<String> excluded =
        appContext.getConfiguration().getAuditLog().getHttpStatus().getExcluded();

    return !excluded.contains("*")
        && !excluded.contains(statusCode)
        && (included.contains("*") || included.contains(statusCode));
  }

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {
    // Return if auditLog is disabled in global config (cfg.yml)
    if (!appContext.getConfiguration().getAuditLog().getEnabled()) {
      return;
    }

    // Return if any of the context objects are missing
    if (Objects.isNull(requestContext) || Objects.isNull(responseContext)) {
      return;
    }

    // Return if the requestId is missing
    Object requestIdObject = requestContext.getProperty("REQUEST_ID");
    if (!(requestIdObject instanceof String requestId)) {
      return;
    }

    // Return if no log is available (for example if the log was aborted during another step)
    if (!auditLog.logIsAvailable(requestId)) {
      return;
    }

    // Abort log and return if HTTP-Code is not applicable according to the global config
    if (!sufficientHttpCode(responseContext.getStatus())) {
      auditLog.abortLog(requestId);
      return;
    }

    // Log status and write log
    auditLog.setOperationStatus(requestId, Integer.toString(responseContext.getStatus()));
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
