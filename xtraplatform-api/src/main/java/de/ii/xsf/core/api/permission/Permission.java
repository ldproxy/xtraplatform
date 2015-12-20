package de.ii.xsf.core.api.permission;

import de.ii.xsf.core.api.Resource;

/**
 *
 * @author fischer
 */
public interface Permission extends Resource {
    public boolean isAllowed(AuthenticatedUser authUser);
}
