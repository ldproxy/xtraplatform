/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.xsf.core.api.permission;

/**
 *
 * @author zahnen
 */
public enum Role {

    NONE("NONE"),
    USER("USER"),
    PUBLISHER("PUBLISHER"),
    ADMINISTRATOR("ADMINISTRATOR"),
    SUPERADMINISTRATOR("SUPERADMINISTRATOR");

    private final String stringRepresentation;

    private Role(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String toString() {
        return stringRepresentation;
    }

    public static Role fromString(String version) {
        for (Role v : Role.values()) {
            if (v.toString().equals(version)) {
                return v;
            }
        }
        return null;
    }

    public boolean isGreaterOrEqual(Role other) {
        return this.compareTo(other) >= 0;
    }
    
    public boolean isGreater(Role other) {
        return this.compareTo(other) > 0;
    }
}
