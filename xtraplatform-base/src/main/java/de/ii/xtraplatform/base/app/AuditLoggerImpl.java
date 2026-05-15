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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@AutoBind
public class AuditLoggerImpl implements AuditLogger {
  private final Map<String, AuditLog> auditLogMapping = new ConcurrentHashMap<>();

  @Inject
  AuditLoggerImpl() {}

  private AuditLog lazyInitOrGetAuditLog(String requestUuid) {
    return auditLogMapping.computeIfAbsent(requestUuid, k -> new AuditLogImpl());
  }

  @Override
  public void initUser(String requestUuid, String user) {
    lazyInitOrGetAuditLog(requestUuid).initUser(user);
  }

  @Override
  public void initType(String requestUuid, String type) {
    lazyInitOrGetAuditLog(requestUuid).initType(type);
  }

  @Override
  public void initPropertyToValueTrack(String requestUuid, String property) {
    lazyInitOrGetAuditLog(requestUuid).initPropertyToValueTrack(property);
  }

  @Override
  public void appendPropertyValue(String requestUuid, String property, String value) {
    lazyInitOrGetAuditLog(requestUuid).appendPropertyValue(property, value);
  }

  @Override
  public void saveToFileAndRemove(String requestUuid) {
    // ToDo Implement
  }

  private static class AuditLogImpl implements AuditLogger.AuditLog {
    private String user;
    private String type;
    private final Map<String, Set<String>> valueLog = new LinkedHashMap<>();

    AuditLogImpl() {}

    @Override
    public void initUser(String user) {
      // ToDo Safety checks
      this.user = user;
    }

    @Override
    public void initType(String type) {
      // ToDo Safety checks
      this.type = type;
    }

    @Override
    public void initPropertyToValueTrack(String property) {
      valueLog.computeIfAbsent(property, p -> new LinkedHashSet<>());
    }

    @Override
    public void appendPropertyValue(String property, String value) {
      if (valueLog.containsKey(property)) {
        valueLog.get(property).add(value);
      }
    }

    @Override
    public String toJson() {
      // ToDo implement
      return user + type;
    }
  }
}
