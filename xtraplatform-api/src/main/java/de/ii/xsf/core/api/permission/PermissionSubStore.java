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
