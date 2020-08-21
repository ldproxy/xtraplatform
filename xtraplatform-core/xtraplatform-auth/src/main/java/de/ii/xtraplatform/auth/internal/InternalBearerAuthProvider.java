/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.internal;

import com.google.common.cache.CacheBuilderSpec;
import com.google.common.collect.Lists;
import de.ii.xtraplatform.dropwizard.api.AuthProvider;
import de.ii.xtraplatform.auth.api.TokenHandler;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.auth.api.UserAuthorizer;
import de.ii.xtraplatform.dropwizard.api.Dropwizard;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.CachingAuthenticator;
import io.dropwizard.auth.chained.ChainedAuthFilter;
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
        @StaticServiceProperty(name = "ranking", type = "int", value = "11")})
@Instantiate
public class InternalBearerAuthProvider implements AuthProvider<User> {

    @Requires
    private TokenHandler tokenHandler;

    @Requires
    Dropwizard dropwizard;

    @Override
    public AuthDynamicFeature getAuthDynamicFeature() {
        JwtTokenAuthenticator tokenAuthenticator = new JwtTokenAuthenticator(tokenHandler);

        CachingAuthenticator<String, User> cachingAuthenticator = new CachingAuthenticator<String, User>(
                dropwizard.getEnvironment()
                          .metrics(), tokenAuthenticator,
                CacheBuilderSpec.parse("maximumSize=10000, expireAfterAccess=10m"));


        AuthFilter<String, User> authFilter = new SplitCookieCredentialAuthFilter.Builder<User>()
                .setAuthenticator(cachingAuthenticator)
                .setAuthorizer(new UserAuthorizer())
                .buildAuthFilter();

        AuthFilter<String, User> authFilter2 = new OAuthCredentialAuthFilter.Builder<User>()
                .setAuthenticator(cachingAuthenticator)
                .setAuthorizer(new UserAuthorizer())
                .setPrefix("Bearer")
                .buildAuthFilter();

        return new AuthDynamicFeature(new ChainedAuthFilter<String, User>(Lists.newArrayList(authFilter, authFilter2)));
    }

    @Override
    public AuthValueFactoryProvider.Binder<User> getAuthValueFactoryProvider() {
        return new AuthValueFactoryProvider.Binder<>(User.class);
    }
}
