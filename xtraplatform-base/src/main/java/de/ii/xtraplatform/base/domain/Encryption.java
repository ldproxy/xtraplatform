/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

public interface Encryption {
  boolean isEnabled();

  byte[] encrypt(String data);

  default String decrypt(byte[] data) {
    return decrypt(data, "");
  }

  String decrypt(byte[] data, String errorContext);
}
