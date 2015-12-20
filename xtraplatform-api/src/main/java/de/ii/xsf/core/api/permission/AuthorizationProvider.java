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
