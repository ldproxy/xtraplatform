package de.ii.xtraplatform.auth.internal;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.auth.api.Role;
import de.ii.xtraplatform.entities.domain.AbstractPersistentEntity;
import de.ii.xtraplatform.entities.domain.EntityComponent;
import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.handler.Entity;
import de.ii.xtraplatform.entities.domain.EntityDataBuilder;
import org.immutables.value.Value;

/**
 *
 * @author zahnen
 */

@EntityComponent
@Entity(type = User.ENTITY_TYPE, dataClass = User.UserData.class)
public class User extends AbstractPersistentEntity<User.UserData> {

    public static final String ENTITY_TYPE = "users";

    @Override
    public String getType() {
        return ENTITY_TYPE;
    }

    @Value.Immutable
    @Value.Modifiable
    @Value.Style(builder = "new")
    @JsonDeserialize(builder = ImmutableUserData.Builder.class)
    public interface UserData extends EntityData {

        abstract class Builder implements EntityDataBuilder<UserData> {}

        String getPassword();

        Role getRole();

    }

    /*private String username;
    private String password;
    private String email;
    private String realname;
    private String description;
    private Role role;
    private boolean superadmin;

    public User() {
        superadmin = false;
    }

    public User(String username) {
        this.username = username;
    }

    @JsonView(JsonViews.RessourceView.class)
    @Override
    public String getResourceId() {
        return username;
    }

    @JsonView(JsonViews.RessourceView.class)
    @Override
    public void setResourceId(String username) {
        this.username = username;
    }

    @JsonView(JsonViews.RessourceView.class)
    public String getUsername() {
        return username;
    }

    @JsonView(JsonViews.RessourceView.class)
    public void setUsername(String username) {
        this.username = username;
    }

    // the Password Hash must not leave the Server!
    @JsonView(JsonViews.StoreView.class)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRawPassword(String password) {
        if (password != null && !password.isEmpty()) {
            try {
                this.password = PasswordHash.createHash(password);
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvalidKeySpecException ex) {
                Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public boolean authenticate(String password) {
        try {
            return PasswordHash.validatePassword(password, this.password);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(User.class
                    .getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeySpecException ex) {
            Logger.getLogger(User.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRealname() {
        return realname;
    }

    public void setRealname(String realname) {
        this.realname = realname;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSuperadmin() {
        return superadmin;
    }

    public void setSuperadmin(boolean superadmin) {
        this.superadmin = superadmin;
    }

    @JsonIgnore
    public Role getRole() {
        if (this.isSuperadmin()) {
            return Role.SUPERADMINISTRATOR;
        }

        return role;
    }

    @JsonIgnore
    public void setRole(Role role) {
        this.role = role;
    }*/
}
