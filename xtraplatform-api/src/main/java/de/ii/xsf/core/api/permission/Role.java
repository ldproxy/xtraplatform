package de.ii.xsf.core.api.permission;

/**
 *
 * @author zahnen
 */
public enum Role {

    NONE("NONE"),
    USER("USER"),
    PUBLISHER("PUBLISHER"),
    ADMINISTRATOR("ADMINISTRATOR"),
    SUPERADMINISTRATOR("SUPERADMINISTRATOR");

    private final String stringRepresentation;

    private Role(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String toString() {
        return stringRepresentation;
    }

    public static Role fromString(String version) {
        for (Role v : Role.values()) {
            if (v.toString().equals(version)) {
                return v;
            }
        }
        return null;
    }

    public boolean isGreaterOrEqual(Role other) {
        return this.compareTo(other) >= 0;
    }
    
    public boolean isGreater(Role other) {
        return this.compareTo(other) > 0;
    }
}
