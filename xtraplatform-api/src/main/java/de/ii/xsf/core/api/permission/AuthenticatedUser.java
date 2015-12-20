package de.ii.xsf.core.api.permission;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author fischer
 */
public class AuthenticatedUser {
    
    private String id;
    private Role role;
    private String orgId;
    private List<String> groups;

    public AuthenticatedUser() {
        this.role = Role.NONE;
        this.groups = new ArrayList<>();
    }
    
    public AuthenticatedUser(String id) {
        this();
        this.id = id;
    }
    
    public AuthenticatedUser(String orgId, String id) {
        this(id);
        this.orgId = orgId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }
    
}
