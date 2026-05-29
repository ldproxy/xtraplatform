/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.blobs.domain.ResourceStore;
import de.ii.xtraplatform.services.domain.AuditLog;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MultivaluedMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class AuditLogImpl implements AuditLog {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogImpl.class);
  private final ObjectMapper objectMapper;
  private final Map<String, Log> auditLogMapping = new ConcurrentHashMap<>();
  private final ResourceStore auditLogStore;

  @Inject
  AuditLogImpl(Jackson jackson, ResourceStore resourceStore) {
    this.objectMapper = jackson.getDefaultObjectMapper();
    this.auditLogStore = resourceStore.writableWith("logs", "audit");
  }

  private Log lazyInitOrGetAuditLog(String requestId) {
    return auditLogMapping.computeIfAbsent(requestId, k -> new LogImpl(requestId));
  }

  @Override
  public void setApi(String requestId, String api) {
    lazyInitOrGetAuditLog(requestId).setApi(api);
  }

  @Override
  public void setActor(String requestId, String actorType, String actorId) {
    lazyInitOrGetAuditLog(requestId).setActor(actorType, actorId);
  }

  @Override
  public void setOperationMethod(String requestId, String method) {
    lazyInitOrGetAuditLog(requestId).setOperationMethod(method);
  }

  @Override
  public void setOperationPath(String requestId, String path) {
    lazyInitOrGetAuditLog(requestId).setOperationPath(path);
  }

  @Override
  public void setOperationHeaders(String requestId, MultivaluedMap<String, String> headers) {
    lazyInitOrGetAuditLog(requestId).setOperationHeaders(headers);
  }

  @Override
  public void setOperationStatus(String requestId, String status) {
    lazyInitOrGetAuditLog(requestId).setOperationStatus(status);
  }

  @Override
  public void setTarget(String requestId, Map<String, Object> target) {
    lazyInitOrGetAuditLog(requestId).setTarget(target);
  }

  @Override
  public void writeAndRemoveLog(String requestId) {
    Log log = auditLogMapping.remove(requestId);
    if (Objects.isNull(log)) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("No AuditLog-object found for requestId {}", requestId);
      }
      return;
    }

    log.finish();

    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(objectMapper.writeValueAsString(log));
      }
      auditLogStore.put(
          Path.of(log.getStarted() + "_" + requestId + ".json"),
          new ByteArrayInputStream(objectMapper.writeValueAsBytes(log)));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize log " + requestId, e);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to write log " + requestId, e);
    }
  }

  @JsonPropertyOrder({"id", "started", "finished", "api", "actor", "operation", "target"})
  public static class LogImpl implements Log {
    private final String id;
    private final Instant started;
    private final Map<String, String> actor = new LinkedHashMap<>();
    private final Map<String, Object> operation = new LinkedHashMap<>();
    private Instant finished;
    private Map<String, Object> target;
    private String api;

    LogImpl(String id) {
      this.id = id;
      this.started = Instant.now();
    }

    @Override
    public void finish() {
      finished = Instant.now();
    }

    @Override
    public void setApi(String api) {
      if (Objects.isNull(this.api)) {
        this.api = api;
      }
    }

    @Override
    public void setActor(String actorType, String actorId) {
      actor.put("type", actorType);
      actor.put("id", actorId);
    }

    @Override
    public void setOperationMethod(String method) {
      operation.put("method", method);
    }

    @Override
    public void setOperationPath(String path) {
      operation.put("path", path);
    }

    @Override
    public void setOperationHeaders(MultivaluedMap<String, String> headers) {
      Map<String, Object> headersReduced = new HashMap<>();

      headers.forEach(
          (key, values) -> {
            if (!values.isEmpty()) {
              headersReduced.put(key, values.size() == 1 ? values.get(0) : values);
            }
          });

      operation.put("headers", headersReduced);
    }

    @Override
    public void setOperationStatus(String status) {
      operation.put("status", status);
    }

    @Override
    public void setTarget(Map<String, Object> target) {
      this.target = target;
    }

    @JsonProperty("id")
    @Override
    public String getId() {
      return id;
    }

    @JsonProperty("started")
    @Override
    public String getStarted() {
      return started.toString();
    }

    @JsonProperty("finished")
    @Override
    public String getFinished() {
      return finished.toString();
    }

    @JsonProperty("api")
    @Override
    public String getApi() {
      return api;
    }

    @JsonProperty("actor")
    @Override
    public Map<String, String> getActor() {
      return actor;
    }

    @JsonProperty("operation")
    @Override
    public Map<String, Object> getOperation() {
      return operation;
    }

    @JsonProperty("target")
    @Override
    public Map<String, Object> getTarget() {
      return target;
    }
  }
}
