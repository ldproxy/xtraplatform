package de.ii.xtraplatform.auth.api;

import java.util.Optional;

public interface TokenHandler {

    String generateToken(User user, int expiresIn);

    Optional<User> parseToken(String token);
}
