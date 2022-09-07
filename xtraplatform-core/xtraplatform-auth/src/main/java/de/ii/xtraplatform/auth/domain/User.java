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
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Value.Immutable
public interface User extends Principal {

  List<String> getScopes();

  @Value.Default
  default Role getRole() {
    return Role.NONE;
  }

  @Value.Default
  default boolean getForceChangePassword() {
    return false;
  }
}
