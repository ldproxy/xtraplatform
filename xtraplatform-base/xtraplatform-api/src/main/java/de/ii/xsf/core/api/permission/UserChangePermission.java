/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.api.permission;

import com.google.common.base.Joiner;

/**
 *
 * @author fischer
 */
public class UserChangePermission implements Permission {
    public static final String PERMISSION_TYPE = "admin-users";
    
    private String resourceId;
    private String userId;
    private Role role;

    public UserChangePermission() {
        this.role = Role.NONE;
    }
    
    public UserChangePermission(String userId, Role role) {
        this.resourceId = Joiner.on('-').join(PERMISSION_TYPE, userId);
        this.userId = userId;
        this.role = role;
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
        if ((userId != null && userId.equals(authUser.getId()))
                || (authUser.getRole().isGreaterOrEqual(Role.ADMINISTRATOR) && authUser.getRole().isGreater(role))) {
            return true;
        }
        return false;
    }

    public String getUser() {
        return userId;
    }

    public void setUser(String userId) {
        this.userId = userId;
    }
    

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
        
}
