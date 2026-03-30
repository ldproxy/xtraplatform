/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.domain;

import org.immutables.value.Value;

@Value.Immutable
public interface TokenResponse {

  String getAccessToken();

  @Value.Default
  default int getExpiresIn() {
    return 60;
  }
}
