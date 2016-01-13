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

import java.io.IOException;
import java.util.List;

/**
 *
 * @author fischer
 */
public interface RightsManagement {

    /**
     * adds a superadmin to the instance. This method should only called once as
     * there is only one superadmin in one instance.
     *
     * @param name the username for the superadmin
     * @param password the password for the superadmin
     * @throws java.io.IOException
     */
    public void addSuperadmin(String name, String password) throws IOException;

    /**
     * is there already a superadmin?
     *
     * @return true if the instance already has a superadmin added
     */
    public boolean hasSuperadmin();

    /**
     *
     * @param authUser for multitenancy
     * @return all users in the given organization
     */
    public List<String> getUsers(AuthenticatedUser authUser);

    /**
     *
     * @param authUser for multitenancy
     * @param username
     * @return
     */
    public boolean hasUser(AuthenticatedUser authUser, String username);

    /**
     *
     * @param authUser for multitenancy
     * @param username
     * @return
     */
    public User getUser(AuthenticatedUser authUser, String username);

    /**
     *
     * @param authUser for multitenancy
     * @param newUser
     * @throws java.io.IOException
     */
    public void addUser(AuthenticatedUser authUser, User newUser) throws IOException;

    /**
     *
     * @param authUser for multitenancy
     * @param username
     * @throws java.io.IOException
     */
    public void deleteUser(AuthenticatedUser authUser, String username) throws IOException;

    /**
     *
     * @param authUser for multitenancy
     * @param newUser
     * @throws java.io.IOException
     */
    public void updateUser(AuthenticatedUser authUser, ProfileUser newUser) throws IOException;

    /**
     *
     * @param authUser for multitenancy
     * @return
     */
    public List<String> getGroups(AuthenticatedUser authUser);

    /**
     *
     * @param authUser for multitenancy
     * @param groupname
     * @return
     */
    public Group getGroup(AuthenticatedUser authUser, String groupname);

    /**
     *
     * @param authUser for multitenancy
     * @param groupname
     * @return
     */
    public boolean hasGroup(AuthenticatedUser authUser, String groupname);

    /**
     *
     * @param authUser for multitenancy
     * @param group
     */
    public void addGroup(AuthenticatedUser authUser, Group group) throws IOException;

    /**
     *
     * @param authUser for multitenancy
     * @param group
     */
    public void updateGroup(AuthenticatedUser authUser, Group group) throws IOException;

    /**
     *
     * @param authUser for multitenancy
     * @param groupname
     * @throws java.io.IOException
     */
    public void deleteGroup(AuthenticatedUser authUser, String groupname) throws IOException;

}
