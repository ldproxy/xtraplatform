/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

public interface AuditLogger {
  interface AuditLog {
    void initUser(String user);

    void initType(String type);

    void initPropertyToValueTrack(String property);

    void appendPropertyValue(String property, String value);

    String toJson();
  }

  void initUser(String requestUuid, String user);

  void initType(String requestUuid, String type);

  void initPropertyToValueTrack(String requestUuid, String property);

  void appendPropertyValue(String requestUuid, String property, String value);

  void saveToFileAndRemove(String requestUuid);
}
