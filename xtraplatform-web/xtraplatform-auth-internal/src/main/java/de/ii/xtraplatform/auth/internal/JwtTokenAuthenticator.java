/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.internal;

import de.ii.xtraplatform.auth.api.TokenHandler;
import de.ii.xtraplatform.auth.api.User;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author zahnen
 */
public class JwtTokenAuthenticator implements Authenticator<String, User> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenAuthenticator.class);

    private final TokenHandler tokenHandler;

    JwtTokenAuthenticator(TokenHandler tokenHandler) {
        this.tokenHandler = tokenHandler;
    }

    @Override
    public Optional<User> authenticate(String token) throws AuthenticationException {

        LOGGER.debug("Authenticating token {}", token);

        return tokenHandler.parseToken(token);
    }
}
