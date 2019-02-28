/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.api;

/**
 * @author zahnen
 */
public enum Role {
    NONE,
    USER,
    EDITOR,
    ADMIN;

    public static Role fromString(String role) {
        for (Role v : Role.values()) {
            if (v.toString().toLowerCase().equals(role.toLowerCase())) {
                return v;
            }
        }
        return NONE;
    }

    public boolean isGreaterOrEqual(Role other) {
        return this.compareTo(other) >= 0;
    }

    public boolean isGreater(Role other) {
        return this.compareTo(other) > 0;
    }
}
