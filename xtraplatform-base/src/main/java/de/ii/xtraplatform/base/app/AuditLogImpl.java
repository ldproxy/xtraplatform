/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AuditLog;
import de.ii.xtraplatform.base.domain.Jackson;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MultivaluedMap;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
// ToDo Evaluate if ConcurrentHashMap is actually needed. Current analysis: Threads could access
// the auditLogMapping at the same time. But there is no scenario in which two threads will access
// the same auditLog object. So apart from the auditLogMapping, no ConcurrentHashMap is needed.
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class AuditLogImpl implements AuditLog {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogImpl.class);
  private final ObjectMapper objectMapper;
  private final Map<String, Log> auditLogMapping = new ConcurrentHashMap<>();

  @Inject
  AuditLogImpl(Jackson jackson) {
    this.objectMapper = jackson.getDefaultObjectMapper();
  }

  private Log lazyInitOrGetAuditLog(String requestId) {
    return auditLogMapping.computeIfAbsent(requestId, k -> new LogImpl(requestId));
  }

  @Override
  public void initApi(String requestId, String api) {
    lazyInitOrGetAuditLog(requestId).initApi(api);
  }

  @Override
  public void initActor(String requestId, String actorType, String actorId) {
    lazyInitOrGetAuditLog(requestId).initActor(actorType, actorId);
  }

  @Override
  public void initOperationMethod(String requestId, String method) {
    lazyInitOrGetAuditLog(requestId).initOperationMethod(method);
  }

  @Override
  public void initOperationPath(String requestId, String path) {
    lazyInitOrGetAuditLog(requestId).initOperationPath(path);
  }

  @Override
  public void initOperationHeaders(String requestId, MultivaluedMap<String, String> headers) {
    lazyInitOrGetAuditLog(requestId).initOperationHeaders(headers);
  }

  @Override
  public void initOperationStatus(String requestId, String status) {
    lazyInitOrGetAuditLog(requestId).initOperationStatus(status);
  }

  @Override
  public void initTarget(String requestId, Map<String, Object> target) {
    lazyInitOrGetAuditLog(requestId).initTarget(target);
  }

  @Override
  public void saveLogToFileAndRemove(String requestId) throws JsonProcessingException {
    if (auditLogMapping.containsKey(requestId)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(objectMapper.writeValueAsString(auditLogMapping.get(requestId)));
      }
      // ToDo save to file
      auditLogMapping.remove(requestId);
    } else {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("No AuditLog-object found for requestId {}", requestId);
      }
    }
  }

  @JsonPropertyOrder({"id", "started", "finished", "api", "actor", "operation", "target"})
  public static class LogImpl implements Log {
    private final String id;
    private final Instant started;
    private final Map<String, String> actor = new ConcurrentHashMap<>();
    private final Map<String, Object> operation = new ConcurrentHashMap<>();
    private Map<String, Object> target;
    private String api;

    LogImpl(String id) {
      this.id = id;
      this.started = Instant.now();
    }

    @JsonProperty("id")
    public String getId() {
      return id;
    }

    @JsonProperty("started")
    public String getStarted() {
      return started.toString();
    }

    // Hier bin ich mir unsicher, ob das so sinnvoll ist.
    @JsonProperty("finished")
    public String finish() {
      return Instant.now().toString();
    }

    @JsonProperty("api")
    public String getApi() {
      return api;
    }

    @JsonProperty("actor")
    public Map<String, String> getActor() {
      return actor;
    }

    @JsonProperty("operation")
    public Map<String, Object> getOperation() {
      return operation;
    }

    @JsonProperty("target")
    public Map<String, Object> getTarget() {
      return target;
    }

    @Override
    public void initApi(String api) {
      if (Objects.isNull(this.api)) {
        this.api = api;
      }
    }

    @Override
    public void initActor(String actorType, String actorId) {
      actor.computeIfAbsent("type", k -> actorType);
      actor.computeIfAbsent("id", k -> actorId);
    }

    @Override
    public void initOperationMethod(String method) {
      operation.computeIfAbsent("method", k -> method);
    }

    @Override
    public void initOperationPath(String path) {
      operation.computeIfAbsent("path", k -> path);
    }

    @Override
    public void initOperationHeaders(MultivaluedMap<String, String> headers) {
      Map<String, Object> headersReduced = new HashMap<>();

      for (String key : headers.keySet()) {
        if (headers.get(key).isEmpty()) {
          continue;
        }

        if (headers.get(key).size() == 1) {
          headersReduced.put(key, headers.get(key).get(0));
          continue;
        }

        headersReduced.put(key, headers.get(key));
      }

      operation.computeIfAbsent("headers", k -> headersReduced);
    }

    @Override
    public void initOperationStatus(String status) {
      operation.computeIfAbsent("status", k -> status);
    }

    @Override
    public void initTarget(Map<String, Object> target) {
      if (Objects.isNull(this.target)) {
        this.target = target;
      }
    }
  }
}
