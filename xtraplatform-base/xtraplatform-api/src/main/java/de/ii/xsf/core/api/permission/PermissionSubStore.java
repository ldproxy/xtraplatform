/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.api.permission;

import java.io.IOException;

/**
 *
 * @author zahnen
 * @param <T>
 */
public interface PermissionSubStore<T extends Permission> {
    public boolean canHandlePermission(String resourceid);
    public T getPermission(AuthenticatedUser authUser, String id);
    public void updatePermission(AuthenticatedUser authUser, T update) throws IOException;
    public void onDeleteUser(AuthenticatedUser authUser, String userId);
    public void onDeleteGroup(AuthenticatedUser authUser, String groupId);
}
