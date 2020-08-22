/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.dropwizard.domain;

import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import java.security.Principal;

/**
 * @author zahnen
 */
public interface AuthProvider<T extends Principal> {
    AuthDynamicFeature getAuthDynamicFeature();

    default Class<?> getRolesAllowedDynamicFeature() {
        return RolesAllowedDynamicFeature.class;
    }

    AuthValueFactoryProvider.Binder<T> getAuthValueFactoryProvider();
}
