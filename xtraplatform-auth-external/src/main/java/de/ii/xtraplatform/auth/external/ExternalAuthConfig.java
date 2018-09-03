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
import java.util.Objects;

import static de.ii.xtraplatform.auth.external.ExternalAuthConfig.*;

/**
 * @author zahnen
 */
@Component
@Provides(specifications = {ExternalAuthConfig.class, AuthConfig.class})
@Instantiate
@LocalBundleConfig(category = "Security", properties = {
        @ConfigPropertyDescriptor(name = JWT_SIGNING_KEY, label = "The signing key for JWT validation"),
        @ConfigPropertyDescriptor(name = USER_INFO_ENDPOINT, label = "The URL to get the user info for a simple token"),
        @ConfigPropertyDescriptor(name = CONNECTION_INFO_ENDPOINT, label = "The URL to get the OpenID Connect Infos", hidden = true),
        @ConfigPropertyDescriptor(name = USER_NAME_KEY, label = "The JSON key of the user name", defaultValue = "name", hidden = true),
        @ConfigPropertyDescriptor(name = USER_ROLE_KEY, label = "The JSON key of the user role", defaultValue = "role", hidden = true),
        @ConfigPropertyDescriptor(name = EXTERNAL_DYNAMIC_AUTHORIZATION_ENDPOINT, label = "The URL of an authorization decider"),
        @ConfigPropertyDescriptor(name = POST_PROCESSING_ENDPOINT, label = "The URL of a postprocessing endpoint")
})
class ExternalAuthConfig extends BundleConfigDefault implements AuthConfig {

    static final String JWT_SIGNING_KEY = "jwtSigningKey";
    static final String USER_INFO_ENDPOINT = "userInfoEndpoint";
    static final String CONNECTION_INFO_ENDPOINT = "openIdConnectEndpoint";
    static final String USER_NAME_KEY = "userNameKey";
    static final String USER_ROLE_KEY = "userRoleKey";
    static final String EXTERNAL_DYNAMIC_AUTHORIZATION_ENDPOINT = "externalDynamicAuthorizationEndpoint";
    static final String POST_PROCESSING_ENDPOINT = "postProcessingEndpoint";

    @Override
    public boolean isJwt() {
        return Objects.nonNull(Strings.emptyToNull(properties.get(JWT_SIGNING_KEY)));
    }

    @Override
    public boolean isActive() {
        try {
            return isJwt() || new URI(getUserInfoUrl().replace("{{token}}", "token")).isAbsolute();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    @Override
    public String getJwtSigningKey() {
        return Strings.nullToEmpty(properties.get(JWT_SIGNING_KEY));
    }

    @Override
    public String getUserInfoUrl() {
        return Strings.nullToEmpty(properties.get(USER_INFO_ENDPOINT));
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

    @Override
    public String getExternalDynamicAuthorizationEndpoint() {
        return Strings.nullToEmpty(properties.get(EXTERNAL_DYNAMIC_AUTHORIZATION_ENDPOINT));
    }

    @Override
    public String getPostProcessingEndpoint() {
        return Strings.nullToEmpty(properties.get(POST_PROCESSING_ENDPOINT));
    }
}