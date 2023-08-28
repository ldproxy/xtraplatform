/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.domain;

import java.net.URI;
import java.security.Key;
import java.util.Map;

public interface Oidc {
  boolean isEnabled();

  URI getLoginUri();

  URI getLogoutUri();

  Map<String, Key> getSigningKeys();
}
