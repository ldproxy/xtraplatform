/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.external;

import com.google.common.cache.CacheBuilderSpec;
import de.ii.xtraplatform.akka.http.Http;
import de.ii.xtraplatform.akka.http.HttpClient;
import de.ii.xtraplatform.dropwizard.api.AuthConfig;
import de.ii.xtraplatform.auth.api.AuthProvider;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.auth.api.UserAuthorizer;
import de.ii.xtraplatform.dropwizard.api.Dropwizard;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.CachingAuthenticator;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;

/**
 * @author zahnen
 */
@Component
@Provides(properties = {@StaticServiceProperty(name = "type", type = "java.lang.String", value = "auth"),
        @StaticServiceProperty(name = "ranking", type = "int", value = "1")})
//TODO: ranking mechanism not working
//@Instantiate
public class ExternalBearerAuthProvider implements AuthProvider<User> {

    private final Dropwizard dropwizard;
    private final HttpClient httpClient;
    private final AuthConfig authConfig;

    public ExternalBearerAuthProvider(@Requires Dropwizard dropwizard, @Requires Http http) {
        this.dropwizard = dropwizard;
        this.httpClient = http.getDefaultClient();
        this.authConfig = dropwizard.getConfiguration().auth;
    }

    @Override
    public AuthDynamicFeature getAuthDynamicFeature() {
        TokenAuthenticator tokenAuthenticator = new TokenAuthenticator(authConfig, httpClient);

        CachingAuthenticator<String, User> cachingAuthenticator = new CachingAuthenticator<String, User>(
                dropwizard.getEnvironment()
                          .metrics(), tokenAuthenticator,
                CacheBuilderSpec.parse("maximumSize=10000, expireAfterAccess=10m"));

        //TODO OAuthEdaAuthFIlter extends OAuthCredentialAuthFilter
        // override filter, get stuff from ContainerRequestContext

        OAuthCredentialAuthFilter<User> authFilter = new OAuthCredentialAuthFilter.Builder<User>()
                .setAuthenticator(cachingAuthenticator)
                .setAuthorizer(new UserAuthorizer())
                .setPrefix("Bearer")
                .buildAuthFilter();

        if (!authConfig.getExternalDynamicAuthorizationEndpoint
                       .isEmpty()) {
            return new AuthDynamicFeature(
                    new ExternalDynamicAuthFilter<>(authConfig.getExternalDynamicAuthorizationEndpoint, authConfig.getPostProcessingEndpoint, httpClient, authFilter)
            );
        }

        return new AuthDynamicFeature(authFilter);
    }

    @Override
    public AuthValueFactoryProvider.Binder<User> getAuthValueFactoryProvider() {
        return new AuthValueFactoryProvider.Binder<>(User.class);
    }
}
