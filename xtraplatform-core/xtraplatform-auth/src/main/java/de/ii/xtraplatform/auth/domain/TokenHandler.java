package de.ii.xtraplatform.auth.domain;

import java.util.Optional;

public interface TokenHandler {

    String generateToken(User user, int expiresIn, boolean rememberMe);

    Optional<User> parseToken(String token);

    <T> Optional<T> parseTokenClaim(String token, String name, Class<T> type);
}
