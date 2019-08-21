package de.ii.xtraplatform.auth.jwt;

import com.google.common.base.Strings;
import de.ii.xtraplatform.auth.api.AuthConfig;
import de.ii.xtraplatform.auth.api.ImmutableUser;
import de.ii.xtraplatform.auth.api.Role;
import de.ii.xtraplatform.auth.api.TokenHandler;
import de.ii.xtraplatform.auth.api.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class JwtTokenHandler implements TokenHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenHandler.class);

    @Requires
    private AuthConfig authConfig;

    @Override
    public String generateToken(User user, int expiresIn) {
        return Jwts.builder()
                   .setSubject(user.getName())
                   .claim(authConfig.getUserRoleKey(), user.getRole()
                                                           .toString())
                   .setExpiration(Date.from(Instant.now()
                                                   .plus(expiresIn, ChronoUnit.MINUTES)))
                   .signWith(getKey())
                   .compact();
    }

    @Override
    public Optional<User> parseToken(String token) {
        if (authConfig.isActive() && authConfig.isJwt()) {
            try {
                Claims claimsJws = Jwts.parser()
                                       .setSigningKey(authConfig.getJwtSigningKey())
                                       .parseClaimsJws(token)
                                       .getBody();

                return Optional.of(ImmutableUser.builder()
                                                .name(claimsJws.getSubject())
                                                .role(Role.fromString(Optional.ofNullable(claimsJws.get(authConfig.getUserRoleKey(), String.class))
                                                                              .orElse("USER")))
                                                .build());
            } catch (Throwable e) {
                //ignore
                LOGGER.debug("Error validating token", e);
            }
        }

        return Optional.empty();
    }

    private Key getKey() {
        return Optional.ofNullable(Strings.emptyToNull(authConfig.getJwtSigningKey()))
                       .map(Base64.getDecoder()::decode)
                       .map(Keys::hmacShaKeyFor)
                       .orElseGet(this::generateKey);
    }

    private SecretKey generateKey() {
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

        authConfig.setJwtSigningKey(Base64.getEncoder()
                                          .encodeToString(key.getEncoded()));

        return key;
    }
}
