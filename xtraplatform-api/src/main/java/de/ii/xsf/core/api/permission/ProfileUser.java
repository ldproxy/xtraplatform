package de.ii.xsf.core.api.permission;

/**
 * helperclass to verify the old/current password
 * 
 * 
 * @author fischer
 */
public class ProfileUser extends User {
    private String oldPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }
        
}
