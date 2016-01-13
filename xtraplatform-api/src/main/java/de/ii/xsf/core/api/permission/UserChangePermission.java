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
