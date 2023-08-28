/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.domain;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Value.Immutable
public interface User extends Principal {

  enum PolicyDecision {
    NONE,
    PERMIT,
    DENY,
    NOT_APPLICABLE
  }

  List<String> getScopes();

  Set<String> getAudience();

  @Value.Default
  default Role getRole() {
    return Role.NONE;
  }

  @Value.Default
  default PolicyDecision getPolicyDecision() {
    return PolicyDecision.NONE;
  }

  @Value.Default
  default boolean getForceChangePassword() {
    return false;
  }
}
