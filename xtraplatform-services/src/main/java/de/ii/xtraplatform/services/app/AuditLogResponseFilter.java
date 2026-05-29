/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.services.domain.AuditLog;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class AuditLogResponseFilter implements ContainerResponseFilter {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogResponseFilter.class);
  private final AuditLog auditLog;

  @Inject
  public AuditLogResponseFilter(AuditLog auditLog) {
    this.auditLog = auditLog;
  }

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {
    if (Objects.isNull(requestContext) || Objects.isNull(responseContext)) {
      return;
    }

    Object requestIdObject = requestContext.getProperty("REQUEST_ID");
    if (Objects.isNull(requestIdObject)) {
      return;
    }

    String requestId = requestIdObject.toString();
    auditLog.setOperationStatus(requestId, Integer.toString(responseContext.getStatus()));
    auditLog.writeAndRemoveLog(requestId);
  }
}
