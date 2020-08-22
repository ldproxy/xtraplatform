/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.domain;

import org.immutables.value.Value;

import java.security.Principal;

/**
 * @author zahnen
 */
@Value.Immutable
public interface User extends Principal {

    @Value.Default
    default Role getRole() {return Role.NONE;}

    @Value.Default
    default boolean getForceChangePassword() {return false;}
}
