/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.external;

import akka.http.javadsl.model.HttpResponse;
import akka.util.ByteString;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xtraplatform.akka.http.AkkaHttp;
import de.ii.xtraplatform.auth.api.AuthConfig;
import de.ii.xtraplatform.auth.api.Role;
import de.ii.xtraplatform.auth.api.User;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

import static de.ii.xtraplatform.api.functional.LambdaWithException.mayThrow;

/**
 * @author zahnen
 */
public class TokenAuthenticator implements Authenticator<String, User> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenAuthenticator.class);

    private final AuthConfig authConfig;
    private final AkkaHttp akkaHttp;

    TokenAuthenticator(AuthConfig authConfig, AkkaHttp akkaHttp) {
        this.authConfig = authConfig;
        this.akkaHttp = akkaHttp;
    }

    @Override
    public Optional<User> authenticate(String token) throws AuthenticationException {
        if (authConfig.isActive()) {
            try {
                if (authConfig.isJwt()) {
                    // validate
                    // parse
                    Claims claimsJws = Jwts.parser()
                                           .setSigningKey(authConfig.getJwtSigningKey())
                                           .parseClaimsJws(token)
                                           .getBody();

                    return Optional.of(new User(claimsJws.getSubject(), Role.fromString(Optional.ofNullable(claimsJws.get(authConfig.getUserRoleKey(), String.class))
                                                                                                .orElse("USER"))));
                } else {
                    // validate/exchange
                    // parse
                    HttpResponse httpResponse = akkaHttp.getResponse(authConfig.getUserInfoUrl()
                                                                               .replace("{{token}}", token))
                                                        .toCompletableFuture()
                                                        .join();
                    if (httpResponse.status()
                                    .isSuccess()) {
                        ObjectMapper mapper = new ObjectMapper();
                        TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {
                        };
                        Map<String, String> userInfo = httpResponse.entity()
                                                                   .getDataBytes()
                                                                   .runFold(ByteString.empty(), ByteString::concat, akkaHttp.getMaterializer())
                                                                   .thenApply(mayThrow(byteString -> (Map<String, String>) mapper.readValue(byteString.utf8String(), typeRef)))
                                                                   .toCompletableFuture()
                                                                   .join();

                        return Optional.of(new User(userInfo.get(authConfig.getUserNameKey()), Role.fromString(Optional.ofNullable(userInfo.get(authConfig.getUserRoleKey()))
                                                                                                                       .orElse("USER"))));
                    }
                }
            } catch (Throwable e) {
                //ignore
                LOGGER.debug("Error validating token", e);
            }
        }

        return Optional.empty();
    }
}
