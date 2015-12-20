package de.ii.xsf.core.api.permission;

/**
 *
 * @author fischer
 */
public class Superadmin extends User {
    
    
    @Override
    public boolean isSuperadmin() {
        return true;
    }
}
