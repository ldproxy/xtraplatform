package de.ii.xsf.core.api.permission;

import de.ii.xsf.core.api.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author fischer
 */
public class Group implements Resource {

    private String name;
    private String description;
    private Role role;
    private List<String> users;

    public Group() {
        this.users = new ArrayList();
    }
    
    @Override
    public String getResourceId() {
        return name;
    }

    @Override
    public void setResourceId(String name) {
        this.name = name;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
