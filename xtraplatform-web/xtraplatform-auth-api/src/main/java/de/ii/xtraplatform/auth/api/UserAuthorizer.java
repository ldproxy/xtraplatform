/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.api;

import io.dropwizard.auth.Authorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
public class UserAuthorizer implements Authorizer<User> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAuthorizer.class);

    @Override
    public boolean authorize(User user, String role) {
        LOGGER.debug("Authorizing {} for role {}", user, role);

        return user.getRole().isGreaterOrEqual(Role.fromString(role));
    }
}
