/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app.external;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import de.ii.xtraplatform.auth.domain.ImmutableUser;
import de.ii.xtraplatform.auth.domain.Role;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.base.domain.AuthConfig;
import de.ii.xtraplatform.web.domain.HttpClient;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
public class TokenAuthenticator implements Authenticator<String, User> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TokenAuthenticator.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();
  private static final TypeReference<Map<String, Object>> TYPE_REF =
      new TypeReference<Map<String, Object>>() {};

  private final AuthConfig authConfig;
  private final HttpClient httpClient;

  TokenAuthenticator(AuthConfig authConfig, HttpClient httpClient) {
    this.authConfig = authConfig;
    this.httpClient = httpClient;
  }

  @Override
  public Optional<User> authenticate(String token) throws AuthenticationException {
    if (authConfig.isActive() && !authConfig.isJwt()) {
      try {
        String url = authConfig.userInfoEndpoint.replace("{{token}}", token);
        InputStream response =
            httpClient.getAsInputStream(url, Map.of("Authorization", "Bearer " + token));

        Map<String, Object> userInfo = MAPPER.readValue(response, TYPE_REF);

        LOGGER.debug("USERINFO {}", userInfo);

        String name = (String) userInfo.get(authConfig.userNameKey);
        Role role =
            Role.fromString(
                Optional.ofNullable((String) userInfo.get(authConfig.userRoleKey)).orElse("USER"));
        List<String> scopes =
            userInfo.get(authConfig.userScopesKey) instanceof String
                ? SPLITTER.splitToList((String) userInfo.get(authConfig.userScopesKey))
                : Optional.ofNullable((List<String>) userInfo.get(authConfig.userScopesKey))
                    .orElse(List.of());

        return Optional.of(ImmutableUser.builder().name(name).role(role).scopes(scopes).build());
      } catch (Throwable e) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Error validating token", e);
        }
      }
    }

    return Optional.empty();
  }
}
