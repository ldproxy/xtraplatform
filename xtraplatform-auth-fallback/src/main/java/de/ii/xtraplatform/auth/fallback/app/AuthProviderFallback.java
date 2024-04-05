/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.fallback.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.auth.domain.UserAuthorizer;
import de.ii.xtraplatform.web.domain.AuthProvider;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class AuthProviderFallback implements AuthProvider<User> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthProviderFallback.class);

  @Inject
  AuthProviderFallback() {
    LOGGER.warn(
        "Authorization features are not available, module xtraplatform-auth was excluded due to the configured module constraints.");
  }

  @Override
  public int getPriority() {
    return 0;
  }

  @Override
  public AuthFilter<String, User> getAuthFilter() {
    Authenticator<String, User> noOpAuthenticator = token -> Optional.empty();

    return new OAuthCredentialAuthFilter.Builder<User>()
        .setAuthenticator(noOpAuthenticator)
        .setAuthorizer(new UserAuthorizer())
        .setPrefix("Bearer")
        .setRealm("xtraplatform")
        .buildAuthFilter();
  }

  @Override
  public AuthValueFactoryProvider.Binder<User> getAuthValueFactoryProvider() {
    return new AuthValueFactoryProvider.Binder<>(User.class);
  }
}
