/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

public interface AuditLogger {
  void initApi(String requestId, String api);

  void initActor(String requestId, String actorType, String actorId);

  void initPropertyToValueTrack(String requestId, String type, String property);

  // ToDo: Evaluate if warnig is justified
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  void appendPropertyValue(String requestId, String type, String property, String value);

  void initPropertyToAccessTrack(String requestId, String type, String property);

  void markAccessed(String requestId, String type, String property);

  void saveToFileAndRemove(String requestId);

  interface AuditLog {
    void initApi(String api);

    void initActor(String actorType, String actorId);

    void initPropertyToValueTrack(String type, String property);

    void appendPropertyValue(String type, String property, String value);

    void initPropertyToAccessTrack(String type, String property);

    void markAccessed(String type, String property);

    String toJson();
  }
}
