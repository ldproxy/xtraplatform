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
import de.ii.xtraplatform.base.domain.AuthConfiguration.IdentityProvider;
import de.ii.xtraplatform.base.domain.AuthConfiguration.Jwt;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.blobs.domain.ResourceStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolver;
import io.jsonwebtoken.jackson.io.JacksonDeserializer;
import io.jsonwebtoken.security.Keys;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;
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
  private static final Splitter LIST_SPLITTER_BLANKS =
      Splitter.on(' ').trimResults().omitEmptyStrings();
  private static final Splitter PATH_SPLITTER = Splitter.on('.');

  private final ResourceStore keyStore;
  private final AuthConfiguration authConfig;
  private final Oidc oidc;
  private final VolatileRegistry volatileRegistry;
  private Key signingKey;
  private IdentityProvider claimsProvider;
  private JwtParser parser;

  @Inject
  public JwtTokenHandler(
      AppContext appContext,
      ResourceStore blobStore,
      Oidc oidc,
      VolatileRegistry volatileRegistry) {
    this.authConfig = appContext.getConfiguration().getAuth();
    this.keyStore = blobStore.with(RESOURCES_JWT);
    this.oidc = oidc;
    this.volatileRegistry = volatileRegistry;
  }

  @Override
  public CompletionStage<Void> onStart(boolean isStartupAsync) {
    if (isStartupAsync) {
      volatileRegistry.onAvailable(keyStore).toCompletableFuture().join();
      volatileRegistry.onAvailable(oidc).toCompletableFuture().join();
    }

    // TODO
    long clockSkew = 3600;

    this.signingKey = getKey();
    this.claimsProvider =
        authConfig.getProviders().values().stream()
            .filter(provider -> provider instanceof IdentityProvider)
            .map(IdentityProvider.class::cast)
            .findFirst()
            .orElse(new IdentityProvider() {});
    this.parser =
        oidc.isEnabled() && oidc instanceof SigningKeyResolver
            ? createParser(null, (SigningKeyResolver) oidc, claimsProvider, clockSkew)
            : createParser(signingKey, null, claimsProvider, clockSkew);

    return CompletableFuture.completedFuture(null);
  }

  private static JwtParser createParser(
      Key key, SigningKeyResolver keyResolver, IdentityProvider claimsProvider, long clockSkew) {
    JwtParserBuilder jwtParserBuilder =
        Jwts.parser()
            .clockSkewSeconds(clockSkew)
            .json(new JacksonDeserializer<>(listType(claimsProvider.getClaims().getAudience())))
            .json(new JacksonDeserializer<>(listType(claimsProvider.getClaims().getScopes())))
            .json(new JacksonDeserializer<>(listType(claimsProvider.getClaims().getPermissions())));

    if (Objects.nonNull(keyResolver)) {
      jwtParserBuilder.setSigningKeyResolver(keyResolver);
    } else {
      jwtParserBuilder.setSigningKey(key);
    }

    return jwtParserBuilder.build();
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
            .claim(
                authConfig.getJwt().get().getClaims().getPermissions(), user.getRole().toString())
            .claim("rememberMe", rememberMe)
            .claim("roles", "")
            .claim("scope", "")
            .claim("aud", "")
            .setExpiration(expiration);
    if (user.getForceChangePassword()) {
      jwtBuilder.claim("forceChangePassword", true);
    }
    return jwtBuilder.signWith(signingKey).compact();
  }

  private static boolean isComplex(String claim) {
    return claim.contains(".");
  }

  private static String baseKey(String claim) {
    return isComplex(claim) ? claim.substring(0, claim.indexOf(".")) : claim;
  }

  private static Map<String, Class<?>> listType(String claim) {
    return Map.of(baseKey(claim), isComplex(claim) ? Map.class : Object.class);
  }

  private String read(Claims claims, String claim) {
    return claims.get(claim, String.class);
  }

  private List<String> readList(Claims claims, String claim) {
    boolean isComplex = isComplex(claim);
    String baseKey = baseKey(claim);
    List<String> subKeys =
        isComplex
            ? PATH_SPLITTER.splitToStream(claim).skip(1).collect(Collectors.toList())
            : List.of();
    List<String> list = new ArrayList<>();

    try {
      if (isComplex) {
        Map<Object, Object> map = claims.get(baseKey, Map.class);
        if (Objects.nonNull(map)) {
          for (int i = 0; i < subKeys.size(); i++) {
            Object entry = map.get(subKeys.get(i));
            if (i == subKeys.size() - 1) {
              list.addAll(parseList(entry, subKeys.get(i)));
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
        Object entry = claims.get(baseKey, Object.class);
        list.addAll(parseList(entry, baseKey));
      }
    } catch (Throwable e) {
      LogContext.error(LOGGER, e, "Claim '{}' cannot be resolved for given token", claim);
    }
    return list;
  }

  private Map<String, Set<String>> readListPerApi(Claims claims, String claim) {
    Map<String, Set<String>> lists = new LinkedHashMap<>();

    if (claim.contains("{{apiId}}")) {
      Pattern pattern = Pattern.compile(claim.replace("{{apiId}}", "(.*)"));
      List<String> claimNames = getClaimNames(claims);

      for (String key : claimNames) {
        if (pattern.matcher(key).matches()) {
          String apiId = pattern.matcher(key).replaceAll("$1");
          List<String> list = readList(claims, key);
          if (!list.isEmpty()) {
            lists.put(apiId, Set.copyOf(list));
          }
        }
      }
    }

    return lists;
  }

  private List<String> getClaimNames(Map<?, ?> claims) {
    List<String> keys = new ArrayList<>();

    for (Object baseKey : claims.keySet()) {
      if (baseKey instanceof String) {
        Object val = claims.get(baseKey);
        List<String> subKeys = new ArrayList<>();

        if (Objects.nonNull(val) && val instanceof Map) {
          subKeys = getClaimNames((Map<?, ?>) val);
        }

        if (subKeys.isEmpty()) {
          keys.add((String) baseKey);
        } else {
          keys.addAll(subKeys.stream().map(subKey -> baseKey + "." + subKey).toList());
        }
      }
    }

    return keys;
  }

  private List<String> parseList(Object entry, String key) {
    if (entry instanceof String) {
      String listString = (String) entry;
      if (listString.startsWith("[") && listString.endsWith("]")) {
        listString = listString.substring(1, listString.length() - 1);
      }
      if (listString.contains(",")) {
        return LIST_SPLITTER.splitToList(listString);
      } else if (listString.contains(" ")) {
        return LIST_SPLITTER_BLANKS.splitToList(listString);
      } else {
        return List.of(listString);
      }
    } else if (entry instanceof Collection<?>) {
      return ((Collection<?>) entry).stream().map(Object::toString).collect(Collectors.toList());
    } else {
      throw new IllegalArgumentException("List or string expected at " + key);
    }
  }

  @Override
  public Optional<User> parseToken(String token) {
    try {
      Claims claimsJws = parser.parseSignedClaims(token).getPayload();
      User user =
          ImmutableUser.builder()
              .name(read(claimsJws, claimsProvider.getClaims().getUserName()))
              .role(Role.fromString(Optional.ofNullable(read(claimsJws, "role")).orElse("USER")))
              .audience(readList(claimsJws, claimsProvider.getClaims().getAudience()))
              .scopes(readList(claimsJws, claimsProvider.getClaims().getScopes()))
              .permissions(readList(claimsJws, claimsProvider.getClaims().getPermissions()))
              .apiPermissions(
                  readListPerApi(claimsJws, claimsProvider.getClaims().getPermissions()))
              .build();

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("USER {}", user);
      }

      return Optional.of(user);
    } catch (Throwable e) {
      LogContext.errorAsDebug(LOGGER, e, "Error validating token");
    }

    return Optional.empty();
  }

  @Override
  public <T> Optional<T> parseTokenClaim(String token, String name, Class<T> type) {
    if (Objects.nonNull(signingKey)) {
      try {
        Claims claimsJws = parser.parseSignedClaims(token).getPayload();

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
    return (!oidc.isEnabled() || oidc.getSigningKeys().isEmpty()
            ? Optional.<Key>empty()
            : Optional.of(oidc.getSigningKeys().values().iterator().next()))
        .or(
            () ->
                authConfig
                    .getJwt()
                    .map(Jwt::getSigningKey)
                    .map(Base64.getDecoder()::decode)
                    .map(Keys::hmacShaKeyFor))
        .or(() -> loadKey().map(Keys::hmacShaKeyFor))
        .orElseGet(this::generateKey);
  }

  private Optional<byte[]> loadKey() {
    try {
      Optional<InputStream> signingKey = keyStore.content(SIGNING_KEY_PATH);

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
    try {
      keyStore.put(SIGNING_KEY_PATH, new ByteArrayInputStream(key.getEncoded()));
    } catch (IOException e) {
      LogContext.error(
          LOGGER, e, "Could not save JWT signing key, tokens will be invalidated on restart");
    }

    return key;
  }
}
