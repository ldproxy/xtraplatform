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
 * an interface to handle authorization for resources
 *
 * @author fischer
 */
public interface AuthorizationProvider {

    /**
     * checks if a user is allowed to access the given resource
     * 
     * @param u the user
     * @param resourceid the resourceid
     * @return true if the user is allowed to access this resource.
     */
    public boolean isAllowed(AuthenticatedUser u, String resourceid);

    /**
     * checks if the role of the user allows to access a resource
     * 
     * @param u the user
     * @param minRole the minimum {@link Role} a {@link AuthenticatedUser} must have to access the resource
     * @return true if the user is allowed to access this resource.
     */
    public boolean isAllowed(AuthenticatedUser u, Role minRole);
}
