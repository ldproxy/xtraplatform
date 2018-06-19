package de.ii.xtraplatform.auth.api;

import io.dropwizard.auth.Authorizer;

/**
 * @author zahnen
 */
public class UserAuthorizer implements Authorizer<User> {
    @Override
    public boolean authorize(User user, String role) {
        return user.getRole().isGreaterOrEqual(Role.fromString(role));
    }
}
