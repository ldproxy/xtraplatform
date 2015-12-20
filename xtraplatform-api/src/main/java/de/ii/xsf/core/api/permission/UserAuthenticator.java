package de.ii.xsf.core.api.permission;

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
