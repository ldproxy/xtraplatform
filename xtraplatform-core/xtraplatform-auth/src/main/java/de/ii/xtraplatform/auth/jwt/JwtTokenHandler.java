package de.ii.xtraplatform.auth.jwt;

import com.google.common.base.Strings;
import de.ii.xtraplatform.dropwizard.api.AuthConfig;
import de.ii.xtraplatform.auth.api.ImmutableUser;
import de.ii.xtraplatform.auth.api.Role;
import de.ii.xtraplatform.auth.api.TokenHandler;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.dropwizard.api.XtraPlatform;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
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

    private final AuthConfig authConfig;

    public JwtTokenHandler(@Requires XtraPlatform xtraPlatform) {
        this.authConfig = xtraPlatform.getConfiguration().auth;
    }

    @Override
    public String generateToken(User user, int expiresIn, boolean rememberMe) {
        JwtBuilder jwtBuilder = Jwts.builder()
                                    .setSubject(user.getName())
                                    .claim(authConfig.getUserRoleKey, user.getRole()
                                                                            .toString())
                                    .claim("rememberMe", rememberMe)
                                    .setExpiration(Date.from(Instant.now()
                                                                    .plus(expiresIn, ChronoUnit.MINUTES)));
        if (user.getForceChangePassword()) {
            jwtBuilder.claim("forceChangePassword", true);
        }
        return jwtBuilder.signWith(getKey())
                         .compact();
    }

    @Override
    public Optional<User> parseToken(String token) {
        if (authConfig.isActive() && authConfig.isJwt()) {
            try {
                Claims claimsJws = Jwts.parser()
                                       .setSigningKey(authConfig.jwtSigningKey)
                                       .parseClaimsJws(token)
                                       .getBody();

                return Optional.of(ImmutableUser.builder()
                                                .name(claimsJws.getSubject())
                                                .role(Role.fromString(Optional.ofNullable(claimsJws.get(authConfig.getUserRoleKey, String.class))
                                                                              .orElse("USER")))
                                                .build());
            } catch (Throwable e) {
                //ignore
                LOGGER.debug("Error validating token", e);
            }
        }

        return Optional.empty();
    }

    @Override
    public <T> Optional<T> parseTokenClaim(String token, String name, Class<T> type) {
        if (authConfig.isActive() && authConfig.isJwt()) {
            try {
                Claims claimsJws = Jwts.parser()
                                       .setSigningKey(authConfig.jwtSigningKey)
                                       .parseClaimsJws(token)
                                       .getBody();

                return Optional.ofNullable(claimsJws.get(name, type));

            } catch (Throwable e) {
                //ignore
                LOGGER.debug("Error validating token", e);
            }
        }

        return Optional.empty();
    }

    private Key getKey() {
        return Optional.ofNullable(Strings.emptyToNull(authConfig.jwtSigningKey))
                       .map(Base64.getDecoder()::decode)
                       .map(Keys::hmacShaKeyFor)
                       .orElseGet(this::generateKey);
    }

    private SecretKey generateKey() {
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

        authConfig.jwtSigningKey = Base64.getEncoder()
                                          .encodeToString(key.getEncoded());

        //TODO
        LOGGER.warn("No valid jwtSigningKey found in cfg.yml, using {}. ", authConfig.jwtSigningKey);

        return key;
    }
}
