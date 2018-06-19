/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.external;

import com.google.common.base.Strings;
import de.ii.xsf.cfgstore.api.BundleConfigDefault;
import de.ii.xsf.cfgstore.api.ConfigPropertyDescriptor;
import de.ii.xsf.cfgstore.api.handler.LocalBundleConfig;
import de.ii.xtraplatform.auth.api.AuthConfig;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.net.URI;
import java.net.URISyntaxException;

import static de.ii.xtraplatform.auth.external.ExternalAuthConfig.*;

/**
 * @author zahnen
 */
@Component
@Provides(specifications = {ExternalAuthConfig.class, AuthConfig.class})
@Instantiate
@LocalBundleConfig(category = "HTML Views", properties = {
        @ConfigPropertyDescriptor(name = IS_JWT, label = "Is the provided token a JSON Web Token?", defaultValue = "false"),
        @ConfigPropertyDescriptor(name = JWT_VALIDATION_ENDPOINT, label = "The URL for JWT validation"),
        @ConfigPropertyDescriptor(name = USER_INFO_ENDPOINT, label = "The URL to get the user info for a simple token"),
        @ConfigPropertyDescriptor(name = CONNECTION_INFO_ENDPOINT, label = "The URL to get the OpenID Connect Infos"),
        @ConfigPropertyDescriptor(name = USER_NAME_KEY, label = "The JSON key of the user name", defaultValue = "name"),
        @ConfigPropertyDescriptor(name = USER_ROLE_KEY, label = "The JSON key of the user role", defaultValue = "role")
})
class ExternalAuthConfig extends BundleConfigDefault implements AuthConfig {

    static final String IS_JWT = "isJwt";
    static final String JWT_VALIDATION_ENDPOINT = "jwtValidationEndpoint";
    static final String USER_INFO_ENDPOINT = "userInfoEndpoint";
    static final String CONNECTION_INFO_ENDPOINT = "openIdConnectEndpoint";
    static final String USER_NAME_KEY = "userNameKey";
    static final String USER_ROLE_KEY = "userRoleKey";

    @Override
    public boolean isJwt() {
        return Boolean.valueOf(Strings.nullToEmpty(properties.get(IS_JWT)));
    }

    @Override
    public String getJwtValidationUrl() {
        return Strings.nullToEmpty(properties.get(JWT_VALIDATION_ENDPOINT));
    }

    @Override
    public String getUserInfoUrl() {
        return Strings.nullToEmpty(properties.get(USER_INFO_ENDPOINT));
    }

    @Override
    public boolean isActive() {
        try {
            return (isJwt() && new URI(getJwtValidationUrl()).isAbsolute()) || new URI(getUserInfoUrl().replace("{{token}}", "token")).isAbsolute();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    @Override
    public String getConnectionInfoEndpoint() {
        return Strings.nullToEmpty(properties.get(CONNECTION_INFO_ENDPOINT));
    }

    @Override
    public String getUserNameKey() {
        return Strings.nullToEmpty(properties.get(USER_NAME_KEY));
    }

    @Override
    public String getUserRoleKey() {
        return Strings.nullToEmpty(properties.get(USER_ROLE_KEY));
    }
}