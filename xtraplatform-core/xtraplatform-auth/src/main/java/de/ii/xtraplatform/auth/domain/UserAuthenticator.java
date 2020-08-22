package de.ii.xtraplatform.auth.domain;

import java.util.Optional;

public interface UserAuthenticator {

    Optional<User> authenticate(String username, String password);
}
