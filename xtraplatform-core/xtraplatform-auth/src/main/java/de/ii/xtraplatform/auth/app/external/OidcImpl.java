/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app.external;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.mayThrow;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.auth.domain.Oidc;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.AuthConfiguration;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.web.domain.Http;
import de.ii.xtraplatform.web.domain.HttpClient;
import io.jsonwebtoken.io.Decoders;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class OidcImpl implements Oidc, AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(OidcImpl.class);

  @Value.Immutable
  @Value.Style(builder = "new")
  @JsonDeserialize(builder = ImmutableOidcConfiguration.Builder.class)
  interface OidcConfiguration {
    @JsonProperty("authorization_endpoint")
    URI getAuthorizationEndpoint();

    @JsonProperty("token_endpoint")
    URI getTokenEndpoint();

    @JsonProperty("userinfo_endpoint")
    URI getUserInfoEndpoint();

    @JsonProperty("end_session_endpoint")
    URI getEndSessionEndpoint();

    @JsonProperty("jwks_uri")
    URI getJwksEndpoint();

    @JsonProperty("grant_types_supported")
    Set<String> getGrantTypes();
  }

  @Value.Immutable
  @Value.Style(builder = "new")
  @JsonDeserialize(builder = ImmutableOidcCerts.Builder.class)
  interface OidcCerts {
    List<Map<String, Object>> getKeys();
  }

  private final AuthConfiguration authConfig;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private Optional<OidcConfiguration> oidcConfiguration;
  private Map<String, Key> signingKeys;
  private boolean enabled;

  @Inject
  public OidcImpl(AppContext appContext, Http http) {
    this.authConfig = appContext.getConfiguration().getAuth();
    this.httpClient = http.getDefaultClient();
    this.objectMapper =
        new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    this.oidcConfiguration = Optional.empty();
    this.signingKeys = Map.of();
    this.enabled = false;
  }

  @Override
  public int getPriority() {
    return 900;
  }

  @Override
  public void onStart() {
    if (authConfig.getOidc().isPresent()) {
      String endpoint = authConfig.getOidc().get().getEndpoint();
      try {
        InputStream inputStream = httpClient.getAsInputStream(endpoint);
        OidcConfiguration oidcConfiguration1 =
            objectMapper.readValue(inputStream, OidcConfiguration.class);

        if (!oidcConfiguration1.getGrantTypes().contains("authorization_code")) {
          LOGGER.error(
              "OpenID Connect endpoint does not support Authorization Code Flow: {}", endpoint);
          return;
        }

        this.oidcConfiguration = Optional.of(oidcConfiguration1);
        this.enabled = true;
      } catch (Throwable e) {
        LogContext.error(LOGGER, e, "Could not parse OpenID Connect configuration at {}", endpoint);
        return;
      }

      String certsEndpoint = oidcConfiguration.get().getJwksEndpoint().toString();
      try {
        InputStream inputStream = httpClient.getAsInputStream(certsEndpoint);
        OidcCerts oidcCerts = objectMapper.readValue(inputStream, OidcCerts.class);

        this.signingKeys =
            oidcCerts.getKeys().stream()
                .map(
                    mayThrow(
                        keyDef -> {
                          if (!keyDef.containsKey("kid")
                              || !keyDef.containsKey("kty")
                              || !keyDef.containsKey("use")
                              || !keyDef.containsKey("n")
                              || !keyDef.containsKey("e")) {
                            return null;
                          }
                          if (!Objects.equals(keyDef.get("kty"), "RSA")) {
                            LOGGER.debug(
                                "Skipping OIDC key '{}' with type '{}', only 'RSA' is supported.",
                                keyDef.get("kid"),
                                keyDef.get("kty"));
                            return null;
                          }
                          if (!Objects.equals(keyDef.get("use"), "sig")) {
                            LOGGER.debug(
                                "Skipping OIDC key '{}' with use '{}', only 'sig' is supported.",
                                keyDef.get("kid"),
                                keyDef.get("use"));
                            return null;
                          }

                          return Map.entry(
                              (String) keyDef.get("kid"),
                              parseRsaKey((String) keyDef.get("n"), (String) keyDef.get("e")));
                        }))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      } catch (Throwable e) {
        LogContext.error(
            LOGGER, e, "Could not parse OpenID Connect certificates at {}", certsEndpoint);
        return;
      }
    }
  }

  private static Key parseRsaKey(String n, String e)
      throws InvalidKeySpecException, NoSuchAlgorithmException {
    BigInteger modulus = new BigInteger(1, Decoders.BASE64URL.decode(n));
    BigInteger publicExponent = new BigInteger(1, Decoders.BASE64URL.decode(e));
    RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus, publicExponent);

    KeyFactory kf = KeyFactory.getInstance("RSA");
    return kf.generatePublic(rsaPublicKeySpec);
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public String getConfigurationUri() {
    return authConfig.getOidc().map(AuthConfiguration.Oidc::getEndpoint).orElse(null);
  }

  @Override
  public URI getLoginUri() {
    return oidcConfiguration.map(OidcConfiguration::getAuthorizationEndpoint).orElse(null);
  }

  @Override
  public URI getTokenUri() {
    return oidcConfiguration.map(OidcConfiguration::getTokenEndpoint).orElse(null);
  }

  @Override
  public URI getLogoutUri() {
    return oidcConfiguration.map(OidcConfiguration::getEndSessionEndpoint).orElse(null);
  }

  @Override
  public String getClientId() {
    return authConfig.getOidc().map(AuthConfiguration.Oidc::getClientId).orElse(null);
  }

  @Override
  public Optional<String> getClientSecret() {
    return authConfig.getOidc().flatMap(AuthConfiguration.Oidc::getClientSecret);
  }

  @Override
  public Map<String, Key> getSigningKeys() {
    return signingKeys;
  }
}
