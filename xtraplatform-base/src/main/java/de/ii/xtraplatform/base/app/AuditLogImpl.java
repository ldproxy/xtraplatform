/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AuditLog;
import de.ii.xtraplatform.base.domain.Jackson;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MultivaluedMap;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
// ToDo Evaluate if ConcurrentHashMap is actually needed. Current analysis: Threads could access
// the auditLogMapping at the same time. But there is no scenario in which two threads will access
// the same auditLog object. So apart from the auditLogMapping, no ConcurrentHashMap is needed.
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class AuditLogImpl implements AuditLog {
  private final ObjectMapper objectMapper;
  private final Map<String, Log> auditLogMapping = new ConcurrentHashMap<>();

  private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogImpl.class);

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
  public void initPropertyToValueTrack(String requestId, String type, String property) {
    lazyInitOrGetAuditLog(requestId).initPropertyToValueTrack(type, property);
  }

  // ToDo: Evaluate if warning is justified
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  @Override
  public void appendPropertyValue(String requestId, String type, String property, String value) {
    lazyInitOrGetAuditLog(requestId).appendPropertyValue(type, property, value);
  }

  @Override
  public void initPropertyToAccessTrack(String requestId, String type, String property) {
    lazyInitOrGetAuditLog(requestId).initPropertyToAccessTrack(type, property);
  }

  @Override
  public void markPropertyAccessed(String requestId, String type, String property) {
    lazyInitOrGetAuditLog(requestId).markPropertyAccessed(type, property);
  }

  @Override
  public void saveLogToFileAndRemove(String requestId) throws JsonProcessingException {
    // ToDo Implement
    if (auditLogMapping.containsKey(requestId)) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(
            lazyInitOrGetAuditLog(requestId)
                .toObjectNode(objectMapper.createObjectNode())
                .toString());
      }
      auditLogMapping.remove(requestId);
    } else {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("No AuditLog-object found for requestId {}", requestId);
      }
    }
  }

  private static class LogImpl implements Log {
    private final String id;
    private final Instant started;
    private String api;
    private final Map<String, String> actor = new ConcurrentHashMap<>();
    private final Operation operation;

    private final Map<String, Map<String, Set<String>>> valueLog = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Set<Boolean>>> accessLog = new ConcurrentHashMap<>();

    LogImpl(String id) {
      this.id = id;
      this.started = Instant.now();
      this.operation = new Operation();
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
      operation.method = method;
    }

    @Override
    public void initOperationPath(String path) {
      operation.path = path;
    }

    @Override
    public void initOperationHeaders(MultivaluedMap<String, String> headers) {
      operation.headers = headers;
    }

    @Override
    public void initOperationStatus(String status) {
      operation.status = status;
    }

    @Override
    public void initPropertyToValueTrack(String type, String property) {
      valueLog.computeIfAbsent(type, k -> new ConcurrentHashMap<>());
      valueLog.get(type).computeIfAbsent(property, k -> ConcurrentHashMap.newKeySet());
    }

    @Override
    public void appendPropertyValue(String type, String property, String value) {
      if (valueLog.containsKey(type) && valueLog.get(type).containsKey(property)) {
        valueLog.get(type).get(property).add(value);
      }
    }

    @Override
    public void initPropertyToAccessTrack(String type, String property) {
      accessLog.computeIfAbsent(type, k -> new ConcurrentHashMap<>());
      accessLog.get(type).computeIfAbsent(property, k -> ConcurrentHashMap.newKeySet());
    }

    @Override
    public void markPropertyAccessed(String type, String property) {
      if (accessLog.containsKey(type) && accessLog.get(type).containsKey(property)) {
        accessLog.get(type).get(property).add(true);
      }
    }

    @Override
    public ObjectNode toObjectNode(ObjectNode root) {
      root.put("id", id);
      root.put("started", started.toString());
      // ToDo Evaluate if it is okay to measure the finish time this early
      root.put("finished", Instant.now().toString());

      ObjectNode actorNode = root.putObject("actor");
      putWithCheck(actorNode, "type", actor.get("type"));
      putWithCheck(actorNode, "id", actor.get("id"));

      ObjectNode operationNode = root.putObject("operation");
      putWithCheck(operationNode, "method", operation.method);
      putWithCheck(operationNode, "path", operation.path);
      putMultivaluedMap(operationNode, "headers", operation.headers);
      putWithCheck(operationNode, "status", operation.status);

      ObjectNode targetNode = root.putObject("target");
      putTarget(targetNode, valueLog, accessLog);

      return root;
    }

    private void putWithCheck(ObjectNode root, String key, Object value) {
      if (Objects.nonNull(value)) {
        root.put(key, value.toString());
      } else {
        root.putNull(key);
      }
    }

    private void putCollection(ObjectNode root, String key, Collection collection) {
      if (Objects.isNull(collection)) {
        root.putNull(key);
        return;
      }

      ArrayNode arrayNode = root.putArray(key);
      for (Object item : collection) {
        arrayNode.addPOJO(item);
      }
    }

    private void putMultivaluedMap(
        ObjectNode root, String key, MultivaluedMap<String, String> multiMap) {
      if (Objects.isNull(multiMap)) {
        root.putNull(key);
        return;
      }

      ObjectNode mapNode = root.putObject(key);
      for (String mapKey : multiMap.keySet()) {
        if (multiMap.get(mapKey).isEmpty()) {
          mapNode.putNull(mapKey);
          continue;
        }

        if (multiMap.get(mapKey).size() > 1) {
          putCollection(mapNode, mapKey, multiMap.get(mapKey));
          continue;
        }

        mapNode.put(mapKey, multiMap.getFirst(mapKey));
      }
    }

    @SuppressWarnings("PMD")
    private void putTarget(
        ObjectNode root,
        Map<String, Map<String, Set<String>>> valueLog,
        Map<String, Map<String, Set<Boolean>>> accessLog) {
      // ToDo finish
      ObjectNode typesNode = root.putObject("typesNode");
      for (String type : valueLog.keySet()) {
        continue;
      }
    }

    private static final class Operation {
      private String method;
      private String path;
      private MultivaluedMap<String, String> headers;
      private String status;
    }
  }
}
