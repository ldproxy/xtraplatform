/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.amdatu;

import java.util.ArrayList;
import java.util.List;

public class ResourceEntry {
    private final String m_alias;
    private final List<String> m_paths;

    public ResourceEntry(String alias) {
        m_alias = alias;
        m_paths = new ArrayList<String>();
    }

    public void addPath(String path) {
        m_paths.add(path);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ResourceEntry other = (ResourceEntry) obj;
        if (!m_alias.equals(other.m_alias)) {
            return false;
        }
        if (!m_paths.equals(other.m_paths)) {
            return false;
        }

        return true;
    }

    public String getAlias() {
        return m_alias;
    }

    public List<String> getPaths() {
        return m_paths;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + m_alias.hashCode();
        result = prime * result + m_paths.hashCode();
        return result;
    }
}