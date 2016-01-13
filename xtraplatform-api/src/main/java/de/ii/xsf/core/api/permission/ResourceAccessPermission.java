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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author fischer
 */
public class ResourceAccessPermission implements Permission {
    // Verbindung zwischen Gruppe und Ressource

    public static final String PERMISSION_TYPE = "services";
    
    public enum OBLIGATION {

        PUBLIC,
        PRIVATE,
        PRIVATE_WITH_GROUPS
    }

    private String resourceId;
    private List<String> groups;
    private OBLIGATION obligation;

    public ResourceAccessPermission() {
        this.groups = new ArrayList<>();
        this.obligation = OBLIGATION.PUBLIC;
    }
    
    public ResourceAccessPermission(String id) {
        this();
        this.resourceId = id;
    }
    
    @Override
    public String getResourceId() {
        return resourceId;
    }

    @Override
    public void setResourceId(String id) {
        this.resourceId = id;
    }

    @Override
    public boolean isAllowed(AuthenticatedUser authUser) {
        // user has Role.NONE if oauth2 is deactivated or if not logged in
        boolean anonymous = (authUser.getRole() == Role.NONE);

        if (obligation == OBLIGATION.PUBLIC) {
            return true;
        } else if (!anonymous && obligation == OBLIGATION.PRIVATE) {
            return true;
        } else if (!anonymous && obligation == OBLIGATION.PRIVATE_WITH_GROUPS) {
            return !Collections.disjoint(groups, authUser.getGroups());
        }

        return false;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public OBLIGATION getObligation() {
        return obligation;
    }

    public void setObligation(OBLIGATION obligation) {
        this.obligation = obligation;
    }
}
