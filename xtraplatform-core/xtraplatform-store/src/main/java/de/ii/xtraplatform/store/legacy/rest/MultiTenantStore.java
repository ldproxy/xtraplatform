/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.legacy.rest;

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
