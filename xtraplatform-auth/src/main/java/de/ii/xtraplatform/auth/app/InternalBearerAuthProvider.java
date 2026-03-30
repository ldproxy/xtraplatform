/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app;

import com.codahale.metrics.MetricRegistry;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.google.common.collect.Lists;
import de.ii.xtraplatform.auth.domain.TokenHandler;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.auth.domain.UserAuthorizer;
import de.ii.xtraplatform.base.domain.AppConfiguration;
import de.ii.xtraplatform.web.domain.AuthProvider;
import de.ii.xtraplatform.web.domain.DropwizardPlugin;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.CachingAuthenticator;
import io.dropwizard.auth.chained.ChainedAuthFilter;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.core.setup.Environment;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class InternalBearerAuthProvider implements AuthProvider<User>, DropwizardPlugin {

  private final TokenHandler tokenHandler;
  private MetricRegistry metricRegistry;

  @Inject
  InternalBearerAuthProvider(TokenHandler tokenHandler) {
    this.tokenHandler = tokenHandler;
  }

  @Override
  public void init(AppConfiguration configuration, Environment environment) {
    this.metricRegistry = environment.metrics();
  }

  @Override
  public int getPriority() {
    return 0;
  }

  @Override
  public AuthFilter<String, User> getAuthFilter() {
    JwtTokenAuthenticator tokenAuthenticator = new JwtTokenAuthenticator(tokenHandler);

    CachingAuthenticator<String, User> cachingAuthenticator =
        new CachingAuthenticator<>(
            metricRegistry,
            tokenAuthenticator,
            CaffeineSpec.parse("maximumSize=10000, expireAfterAccess=10m"));

    AuthFilter<String, User> authFilter =
        new SplitCookieCredentialAuthFilter.Builder<User>()
            .setAuthenticator(tokenAuthenticator)
            .setAuthorizer(new UserAuthorizer())
            .setPrefix("Bearer")
            .setRealm("xtraplatform")
            .buildAuthFilter();

    AuthFilter<String, User> authFilter2 =
        new OAuthCredentialAuthFilter.Builder<User>()
            .setAuthenticator(cachingAuthenticator)
            .setAuthorizer(new UserAuthorizer())
            .setPrefix("Bearer")
            .setRealm("xtraplatform")
            .buildAuthFilter();

    return new ChainedAuthFilter<>(Lists.newArrayList(authFilter, authFilter2));
  }

  @Override
  public AuthValueFactoryProvider.Binder<User> getAuthValueFactoryProvider() {
    return new AuthValueFactoryProvider.Binder<>(User.class);
  }
}
