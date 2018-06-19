package de.ii.xtraplatform.auth.api;

import javax.security.auth.Subject;
import java.security.Principal;

/**
 * @author zahnen
 */
public class User implements Principal {

    private final String name;
    private final Role role;

    public User(String name, Role role) {
        this.name = name;
        this.role = role;
    }

    @Override
    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public boolean implies(Subject subject) {
        return false;
    }
}
