/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.web.amdatu;

/**
 * Thrown when {@link ResourceKeyParser} finds an invalid entry.
 */
public class InvalidEntryException extends Exception {
    private final String m_entry;

    public InvalidEntryException(String message, String entry) {
        super(message);
        m_entry = entry;
    }

    public String getEntry() {
        return m_entry;
    }
}