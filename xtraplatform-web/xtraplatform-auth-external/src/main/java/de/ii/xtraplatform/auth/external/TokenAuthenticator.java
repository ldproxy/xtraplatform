/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.external;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xtraplatform.akka.http.HttpClient;
import de.ii.xtraplatform.dropwizard.api.AuthConfig;
import de.ii.xtraplatform.auth.api.ImmutableUser;
import de.ii.xtraplatform.auth.api.Role;
import de.ii.xtraplatform.auth.api.User;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */
public class TokenAuthenticator implements Authenticator<String, User> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenAuthenticator.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> TYPE_REF = new TypeReference<Map<String, String>>() {
    };

    private final AuthConfig authConfig;
    private final HttpClient httpClient;

    TokenAuthenticator(AuthConfig authConfig, HttpClient httpClient) {
        this.authConfig = authConfig;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<User> authenticate(String token) throws AuthenticationException {
        if (authConfig.isActive()) {
            try {
                if (authConfig.isJwt()) {
                    // validate
                    // parse
                    Claims claimsJws = Jwts.parser()
                                           .setSigningKey(authConfig.jwtSigningKey)
                                           .parseClaimsJws(token)
                                           .getBody();

                    return Optional.of(ImmutableUser.builder()
                                                    .name(claimsJws.getSubject())
                                                    .role(Role.fromString(Optional.ofNullable(claimsJws.get(authConfig.getUserRoleKey, String.class))
                                                                                  .orElse("USER")))
                                                    .build());
                } else {
                    // validate/exchange
                    // parse
                    String url = authConfig.getUserInfoEndpoint
                                           .replace("{{token}}", token);
                    InputStream response = httpClient.getAsInputStream(url);

                    Map<String, String> userInfo = MAPPER.readValue(response, TYPE_REF);

                    return Optional.of(ImmutableUser.builder()
                                                    .name(userInfo.get(authConfig.getUserNameKey))
                                                    .role(Role.fromString(Optional.ofNullable(userInfo.get(authConfig.getUserRoleKey))
                                                                                  .orElse("USER")))
                                                    .build());
                }
            } catch (Throwable e) {
                //ignore
                LOGGER.debug("Error validating token", e);
            }
        }

        return Optional.empty();
    }
}
