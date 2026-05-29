/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.domain;

import jakarta.ws.rs.core.MultivaluedMap;
import java.util.Map;

public interface AuditLog {
  void setApi(String requestId, String api);

  void setActor(String requestId, String actorType, String actorId);

  void setOperationMethod(String requestId, String method);

  void setOperationPath(String requestId, String path);

  void setOperationHeaders(String requestId, MultivaluedMap<String, String> headers);

  void setOperationStatus(String requestId, String status);

  void setTarget(String requestId, Map<String, Object> target);

  void writeAndRemoveLog(String requestId);

  interface Log {

    void finish();

    void setApi(String api);

    void setActor(String actorType, String actorId);

    void setOperationMethod(String method);

    void setOperationPath(String path);

    void setOperationHeaders(MultivaluedMap<String, String> headers);

    void setOperationStatus(String status);

    void setTarget(Map<String, Object> target);

    String getId();

    String getStarted();

    String getFinished();

    String getApi();

    Map<String, String> getActor();

    Map<String, Object> getOperation();

    Map<String, Object> getTarget();
  }
}
