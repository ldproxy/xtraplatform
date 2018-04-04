/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
