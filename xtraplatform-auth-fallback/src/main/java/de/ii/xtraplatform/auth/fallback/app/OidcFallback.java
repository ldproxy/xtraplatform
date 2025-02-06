/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.fallback.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.auth.domain.Oidc;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry.ChangeHandler;
import java.net.URI;
import java.security.Key;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class OidcFallback implements Oidc {

  @Inject
  public OidcFallback() {}

  @Override
  public boolean isEnabled() {
    return false;
  }

  @Override
  public String getConfigurationUri() {
    return null;
  }

  @Override
  public URI getLoginUri() {
    return null;
  }

  @Override
  public URI getTokenUri() {
    return null;
  }

  @Override
  public URI getLogoutUri() {
    return null;
  }

  @Override
  public String getClientId() {
    return null;
  }

  @Override
  public Optional<String> getClientSecret() {
    return Optional.empty();
  }

  @Override
  public Map<String, Key> getSigningKeys() {
    return null;
  }

  @Override
  public State getState() {
    return State.AVAILABLE;
  }

  @Override
  public Optional<String> getMessage() {
    return Optional.empty();
  }

  @Override
  public Runnable onStateChange(ChangeHandler handler, boolean initialCall) {
    return () -> {};
  }
}
