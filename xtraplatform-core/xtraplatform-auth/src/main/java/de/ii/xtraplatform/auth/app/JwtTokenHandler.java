/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Splitter;
import de.ii.xtraplatform.auth.domain.ImmutableUser;
import de.ii.xtraplatform.auth.domain.Oidc;
import de.ii.xtraplatform.auth.domain.Role;
import de.ii.xtraplatform.auth.domain.TokenHandler;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.AuthConfiguration;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.StoreSourceFsV3;
import de.ii.xtraplatform.store.domain.BlobStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.jackson.io.JacksonDeserializer;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  private static final Splitter LIST_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();
  private static final Splitter PATH_SPLITTER = Splitter.on('.');

  private final BlobStore keyStore;
  private final AuthConfiguration authConfig;
  private final Oidc oidc;
  private final boolean isOldStoreAndReadOnly;
  private Key signingKey;
  private JwtParser parser;
  private Function<Claims, Set<String>> permissionReader;
  private Function<Claims, Set<String>> audienceReader;

  @Inject
  public JwtTokenHandler(AppContext appContext, BlobStore blobStore, Oidc oidc) {
    this.authConfig = appContext.getConfiguration().getAuth();
    this.keyStore = blobStore.with(RESOURCES_JWT);
    this.oidc = oidc;
    this.isOldStoreAndReadOnly =
        appContext.getConfiguration().getStore().getSources(appContext.getDataDir()).stream()
            .anyMatch(source -> StoreSourceFsV3.isOldDefaultStore(source) && !source.isWritable());
  }

  @Override
  public void onStart() {
    this.signingKey = getKey();

    // TODO
    long clockSkew = 3600;
    String claimsPermissions = authConfig.getClaims().getPermissions();
    boolean isComplex = claimsPermissions.contains(".");
    String permissionsKey =
        isComplex
            ? claimsPermissions.substring(0, claimsPermissions.indexOf("."))
            : claimsPermissions;
    List<String> subKeys =
        isComplex
            ? PATH_SPLITTER.splitToStream(claimsPermissions).skip(1).collect(Collectors.toList())
            : List.of();

    this.parser =
        Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .setAllowedClockSkewSeconds(clockSkew)
            .deserializeJsonWith(
                new JacksonDeserializer(Map.of(permissionsKey, isComplex ? Map.class : List.class)))
            .build();

    this.permissionReader =
        (claims) -> {
          Set<String> permissions = new HashSet<>();
          try {
            if (isComplex) {
              Map<Object, Object> map = claims.get(permissionsKey, Map.class);
              if (Objects.nonNull(map)) {
                for (int i = 0; i < subKeys.size(); i++) {
                  Object entry = map.get(subKeys.get(i));
                  if (i == subKeys.size() - 1) {
                    if (entry instanceof String) {
                      permissions.add((String) entry);
                    } else if (entry instanceof List) {
                      ((List<Object>) entry).forEach(e -> permissions.add(e.toString()));
                    } else {
                      throw new IllegalArgumentException(
                          "List or string expected at " + subKeys.get(i));
                    }
                    break;
                  }
                  if (entry instanceof Map) {
                    map = (Map<Object, Object>) entry;
                  } else {
                    throw new IllegalArgumentException("Map expected at " + subKeys.get(i));
                  }
                }
              }
            } else {
              List<Object> list = claims.get(permissionsKey, List.class);
              if (Objects.nonNull(list)) {
                list.stream().forEach(e -> permissions.add(e.toString()));
              }
            }
          } catch (Throwable e) {
            LogContext.error(
                LOGGER,
                e,
                "Permission key '{}' cannot be resolved for given token",
                claimsPermissions);
          }
          return permissions;
        };

    this.audienceReader =
        (claims) -> {
          if (Objects.isNull(claims.getAudience())) {
            return Set.of();
          }

          String aud = claims.getAudience().trim();
          if (aud.startsWith("[") && aud.endsWith("]")) {
            return LIST_SPLITTER
                .splitToStream(aud.substring(1, aud.length() - 1))
                .collect(Collectors.toSet());
          }
          return Set.of(aud);
        };
  }

  @Override
  public String generateToken(User user, int expiresIn, boolean rememberMe) {
    return generateToken(
        user, Date.from(Instant.now().plus(expiresIn, ChronoUnit.MINUTES)), rememberMe);
  }

  @Override
  public String generateToken(User user, Date expiration, boolean rememberMe) {
    JwtBuilder jwtBuilder =
        Jwts.builder()
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
        Claims claimsJws = parser.parseClaimsJws(token).getBody();

        return Optional.of(
            ImmutableUser.builder()
                .name(claimsJws.get(authConfig.getClaims().getUserName(), String.class))
                .role(
                    Role.fromString(
                        Optional.ofNullable(
                                claimsJws.get(authConfig.getUserRoleKey(), String.class))
                            .orElse("USER")))
                .scopes(permissionReader.apply(claimsJws))
                .audience(audienceReader.apply(claimsJws))
                .build());
      } catch (Throwable e) {
        LogContext.errorAsDebug(LOGGER, e, "Error validating token");
      }
    }

    return Optional.empty();
  }

  @Override
  public <T> Optional<T> parseTokenClaim(String token, String name, Class<T> type) {
    if (Objects.nonNull(signingKey)) {
      try {
        Claims claimsJws = parser.parseClaimsJws(token).getBody();

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
    // TODO: multiple keys?
    return (!oidc.isEnabled() || oidc.getSigningKeys().isEmpty()
            ? Optional.<Key>empty()
            : Optional.of(oidc.getSigningKeys().values().iterator().next()))
        .or(
            () ->
                authConfig
                    .getSimple()
                    .flatMap(simple -> simple.getJwtSigningKey())
                    .map(Base64.getDecoder()::decode)
                    .map(Keys::hmacShaKeyFor))
        .or(() -> loadKey().map(Keys::hmacShaKeyFor))
        .orElseGet(this::generateKey);
  }

  private Optional<byte[]> loadKey() {
    try {
      Optional<InputStream> signingKey = keyStore.get(SIGNING_KEY_PATH);

      if (signingKey.isPresent()) {
        try (InputStream inputStream = signingKey.get()) {
          byte[] bytes = inputStream.readAllBytes();

          return Optional.of(bytes);
        }
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
