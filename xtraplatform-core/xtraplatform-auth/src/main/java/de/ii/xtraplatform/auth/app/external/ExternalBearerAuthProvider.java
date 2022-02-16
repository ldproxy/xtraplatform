/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app.external;

import com.github.benmanes.caffeine.cache.CaffeineSpec;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.auth.domain.UserAuthorizer;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.web.domain.AuthProvider;
import de.ii.xtraplatform.web.domain.Dropwizard;
import de.ii.xtraplatform.base.domain.AuthConfig;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.CachingAuthenticator;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;

/** @author zahnen */
//TODO: HttpClient
//TODO: ranking, which one to use
//TODO: AutoBind
public class ExternalBearerAuthProvider implements AuthProvider<User> {

  private final Dropwizard dropwizard;
  //private final HttpClient httpClient;
  private final AuthConfig authConfig;

  public ExternalBearerAuthProvider(AppContext appContext, Dropwizard dropwizard/*, Http http*/) {
    this.dropwizard = dropwizard;
    //this.httpClient = http.getDefaultClient();
    this.authConfig = appContext.getConfiguration().auth;
  }

  @Override
  public AuthDynamicFeature getAuthDynamicFeature() {
    TokenAuthenticator tokenAuthenticator = new TokenAuthenticator(authConfig/*, httpClient*/);

    CachingAuthenticator<String, User> cachingAuthenticator =
        new CachingAuthenticator<String, User>(
            dropwizard.getEnvironment().metrics(),
            tokenAuthenticator,
            CaffeineSpec.parse("maximumSize=10000, expireAfterAccess=10m"));

    // TODO OAuthEdaAuthFIlter extends OAuthCredentialAuthFilter
    // override filter, get stuff from ContainerRequestContext

    OAuthCredentialAuthFilter<User> authFilter =
        new OAuthCredentialAuthFilter.Builder<User>()
            .setAuthenticator(cachingAuthenticator)
            .setAuthorizer(new UserAuthorizer())
            .setPrefix("Bearer")
            .buildAuthFilter();

    if (!authConfig.getExternalDynamicAuthorizationEndpoint.isEmpty()) {
      return new AuthDynamicFeature(
          new ExternalDynamicAuthFilter<>(
              authConfig.getExternalDynamicAuthorizationEndpoint,
              authConfig.getPostProcessingEndpoint,
              /*httpClient,*/
              authFilter));
    }

    return new AuthDynamicFeature(authFilter);
  }

  @Override
  public AuthValueFactoryProvider.Binder<User> getAuthValueFactoryProvider() {
    return new AuthValueFactoryProvider.Binder<>(User.class);
  }
}
