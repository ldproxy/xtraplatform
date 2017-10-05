/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
