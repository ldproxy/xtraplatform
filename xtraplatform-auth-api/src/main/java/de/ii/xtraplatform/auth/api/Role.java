package de.ii.xtraplatform.auth.api;

/**
 * @author zahnen
 */
public enum Role {
    NONE,
    USER,
    EDITOR,
    ADMIN;

    public static Role fromString(String role) {
        for (Role v : Role.values()) {
            if (v.toString().toLowerCase().equals(role.toLowerCase())) {
                return v;
            }
        }
        return NONE;
    }

    public boolean isGreaterOrEqual(Role other) {
        return this.compareTo(other) >= 0;
    }

    public boolean isGreater(Role other) {
        return this.compareTo(other) > 0;
    }
}
