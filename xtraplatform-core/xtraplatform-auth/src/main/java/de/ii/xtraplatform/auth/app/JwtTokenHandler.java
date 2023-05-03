/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Strings;
import de.ii.xtraplatform.auth.domain.ImmutableUser;
import de.ii.xtraplatform.auth.domain.Role;
import de.ii.xtraplatform.auth.domain.TokenHandler;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.AuthConfiguration;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.StoreSourceDefaultV3;
import de.ii.xtraplatform.store.domain.BlobStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultJwtBuilder;
import io.jsonwebtoken.impl.DefaultJwtParser;
import io.jsonwebtoken.security.Keys;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class JwtTokenHandler implements TokenHandler, AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenHandler.class);
  private static final String RESOURCES_JWT = "jwt";
  private static final Path SIGNING_KEY_PATH = Path.of("signingKey");

  private final BlobStore keyStore;
  private final AuthConfiguration authConfig;
  private final boolean isOldStoreAndReadOnly;
  private Key signingKey;

  @Inject
  public JwtTokenHandler(AppContext appContext, BlobStore blobStore) {
    this.authConfig = appContext.getConfiguration().getAuth();
    this.keyStore = blobStore.with(RESOURCES_JWT);
    this.isOldStoreAndReadOnly =
        appContext.getConfiguration().getStore().getSources().stream()
            .anyMatch(source -> source instanceof StoreSourceDefaultV3 && !source.isWritable());
  }

  @Override
  public void onStart() {
    this.signingKey = getKey();
  }

  @Override
  public String generateToken(User user, int expiresIn, boolean rememberMe) {
    return generateToken(
        user, Date.from(Instant.now().plus(expiresIn, ChronoUnit.MINUTES)), rememberMe);
  }

  @Override
  public String generateToken(User user, Date expiration, boolean rememberMe) {
    JwtBuilder jwtBuilder =
        new DefaultJwtBuilder()
            .setSubject(user.getName())
            .claim(authConfig.getUserRoleKey(), user.getRole().toString())
            .claim("rememberMe", rememberMe)
            .setExpiration(expiration);
    if (user.getForceChangePassword()) {
      jwtBuilder.claim("forceChangePassword", true);
    }
    return jwtBuilder.signWith(signingKey).compact();
  }

  @Override
  public Optional<User> parseToken(String token) {
    if (Objects.nonNull(signingKey)) {
      try {
        Claims claimsJws =
            new DefaultJwtParser().setSigningKey(signingKey).parseClaimsJws(token).getBody();

        return Optional.of(
            ImmutableUser.builder()
                .name(claimsJws.getSubject())
                .role(
                    Role.fromString(
                        Optional.ofNullable(
                                claimsJws.get(authConfig.getUserRoleKey(), String.class))
                            .orElse("USER")))
                .build());
      } catch (Throwable e) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Error validating token", e);
        }
      }
    }

    return Optional.empty();
  }

  @Override
  public <T> Optional<T> parseTokenClaim(String token, String name, Class<T> type) {
    if (Objects.nonNull(signingKey)) {
      try {
        Claims claimsJws =
            new DefaultJwtParser().setSigningKey(signingKey).parseClaimsJws(token).getBody();

        return Optional.ofNullable(claimsJws.get(name, type));

      } catch (Throwable e) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Error validating token", e);
        }
      }
    }

    return Optional.empty();
  }

  private Key getKey() {
    return Optional.ofNullable(Strings.emptyToNull(authConfig.getJwtSigningKey()))
        .map(Base64.getDecoder()::decode)
        .or(this::loadKey)
        .map(Keys::hmacShaKeyFor)
        .orElseGet(this::generateKey);
  }

  private Optional<byte[]> loadKey() {
    try {
      Optional<InputStream> signingKey = keyStore.get(SIGNING_KEY_PATH);

      if (signingKey.isPresent()) {
        byte[] bytes = signingKey.get().readAllBytes();

        return Optional.of(bytes);
      }
    } catch (IOException e) {
      LogContext.error(LOGGER, e, "Could not load JWT signing key");
    }

    return Optional.empty();
  }

  private SecretKey generateKey() {
    SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // TODO: either throw in put when no writable source or return true if written
    if (!isOldStoreAndReadOnly) {
      try {
        keyStore.put(SIGNING_KEY_PATH, new ByteArrayInputStream(key.getEncoded()));
      } catch (IOException e) {
        LogContext.error(
            LOGGER, e, "Could not save JWT signing key, tokens will be invalidated on restart");
      }
    }

    return key;
  }
}
