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
package de.ii.xsf.core.web.amdatu;

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