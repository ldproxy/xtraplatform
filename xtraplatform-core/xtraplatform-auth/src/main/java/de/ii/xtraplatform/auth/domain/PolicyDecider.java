/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.domain;

import java.util.Map;
import java.util.Optional;

public interface PolicyDecider {
  PolicyDecision request(
      String resourceId,
      Map<String, Object> resourceAttributes,
      String actionId,
      Map<String, Object> actionAttributes,
      Optional<User> user);
}
