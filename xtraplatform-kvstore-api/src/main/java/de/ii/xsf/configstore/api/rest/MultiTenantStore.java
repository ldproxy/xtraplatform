package de.ii.xsf.configstore.api.rest;

import de.ii.xsf.core.api.Resource;
import de.ii.xsf.core.api.organization.OrganizationDecider;
import de.ii.xsf.core.api.permission.AuthenticatedUser;

/**
 *
 * @author zahnen
 */
public class MultiTenantStore {

    public static <T extends ResourceStore> T forUser(T store, AuthenticatedUser user) {
        return forOrgId(store, user.getOrgId());
    }

    public static <T extends ResourceStore> T forOrgId(T store, String orgId) {
        if (!OrganizationDecider.isRootOrg(orgId)) {
            return (T) store.withParent(orgId);
        }

        return store;
    }

    private static boolean isRootOrg(String orgid) {
        if (orgid == null) {
            return true;
        }

        return false;
    }
}
