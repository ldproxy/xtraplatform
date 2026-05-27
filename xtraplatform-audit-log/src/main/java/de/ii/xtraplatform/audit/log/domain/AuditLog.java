/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.audit.log.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.Map;

public interface AuditLog {
  void initApi(String requestId, String api);

  void initActor(String requestId, String actorType, String actorId);

  void initOperationMethod(String requestId, String method);

  void initOperationPath(String requestId, String path);

  void initOperationHeaders(String requestId, MultivaluedMap<String, String> headers);

  void initOperationStatus(String requestId, String status);

  void initTarget(String requestId, Map<String, Object> target);

  void saveLogToFileAndRemove(String requestId) throws JsonProcessingException;

  interface Log {
    void initApi(String api);

    void initActor(String actorType, String actorId);

    void initOperationMethod(String method);

    void initOperationPath(String path);

    void initOperationHeaders(MultivaluedMap<String, String> headers);

    void initOperationStatus(String status);

    void initTarget(Map<String, Object> target);
  }
}
