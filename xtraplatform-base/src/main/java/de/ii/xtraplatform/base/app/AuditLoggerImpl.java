/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AuditLogger;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class AuditLoggerImpl implements AuditLogger {
  private final Map<String, AuditLog> auditLogMapping = new ConcurrentHashMap<>();

  private static final Logger LOGGER = LoggerFactory.getLogger(AuditLoggerImpl.class);

  @Inject
  AuditLoggerImpl() {}

  private AuditLog lazyInitOrGetAuditLog(String requestId) {
    return auditLogMapping.computeIfAbsent(requestId, k -> new AuditLogImpl(requestId));
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
  public void initPropertyToValueTrack(String requestId, String type, String property) {
    lazyInitOrGetAuditLog(requestId).initPropertyToValueTrack(type, property);
  }

  // ToDo: Evaluate if warnig is justified
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
  public void saveToFileAndRemove(String requestId) {
    // ToDo Implement
    if (auditLogMapping.containsKey(requestId)) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(lazyInitOrGetAuditLog(requestId).toJson());
      }
      auditLogMapping.remove(requestId);
    } else {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("No AuditLog-object found for requestId " + requestId);
      }
    }
  }

  private static class AuditLogImpl implements AuditLogger.AuditLog {
    private final String id;
    private final Instant started;
    private String api;
    private final Map<String, String> actor = new LinkedHashMap<>();

    private final Map<String, Map<String, Set<String>>> valueLog = new LinkedHashMap<>();
    private final Map<String, Map<String, Set<Boolean>>> accessLog = new LinkedHashMap<>();

    AuditLogImpl(String id) {
      this.id = id;
      this.started = Instant.now();
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
    public void initPropertyToValueTrack(String type, String property) {
      valueLog.computeIfAbsent(type, k -> new LinkedHashMap<>());
      valueLog.get(type).computeIfAbsent(property, k -> new LinkedHashSet<>());
    }

    @Override
    public void appendPropertyValue(String type, String property, String value) {
      if (valueLog.containsKey(type) && valueLog.get(type).containsKey(property)) {
        valueLog.get(type).get(property).add(value);
      }
    }

    @Override
    public void initPropertyToAccessTrack(String type, String property) {
      accessLog.computeIfAbsent(type, k -> new LinkedHashMap<>());
      accessLog.get(type).computeIfAbsent(property, k -> new LinkedHashSet<>());
    }

    @Override
    public void markPropertyAccessed(String type, String property) {
      if (accessLog.containsKey(type) && accessLog.get(type).containsKey(property)) {
        accessLog.get(type).get(property).add(true);
      }
    }

    @Override
    public String toJson() {
      Instant finished = Instant.now();
      // ToDo implement
      return "\n------------------\nid: "
          + id
          + "\nstarted: "
          + started
          + "\nfinished: "
          + finished
          + "\napi: "
          + api
          + "\nactor: "
          + actor
          + "\naccess-track: "
          + accessLog
          + "\nvalue-track: "
          + valueLog
          + "\n------------------\n";
    }
  }
}
