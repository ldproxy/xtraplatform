/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.api.permission;

/**
 *
 * @author fischer
 */
public interface UserAuthenticator {
    
    /**
     * authenticates a user 
     * 
     * @param authUser
     * @param password
     * @return a {@link AuthenticatedUser} if the credentials are valid, null otherwise
     */
    public AuthenticatedUser authenticate(AuthenticatedUser authUser, String password); 

    /**
     * Get an {@link AuthenticatedUser}
     * 
     * @param authUser
     * @return a {@link AuthenticatedUser}
     */
    public AuthenticatedUser verifyAuthenticatedUser(AuthenticatedUser authUser);
}
