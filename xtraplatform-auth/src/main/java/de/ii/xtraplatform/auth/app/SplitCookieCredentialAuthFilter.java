/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app;

import de.ii.xtraplatform.auth.domain.SplitCookie;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import java.io.IOException;
import java.security.Principal;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
@Priority(Priorities.AUTHENTICATION)
public final class SplitCookieCredentialAuthFilter<P extends Principal>
    extends AuthFilter<String, P> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(SplitCookieCredentialAuthFilter.class);

  private SplitCookieCredentialAuthFilter() {
    super();
  }

  @Override
  public void filter(final ContainerRequestContext requestContext) throws IOException {
    String credentials = SplitCookie.readToken(requestContext.getCookies()).orElse(null);

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("credentials {}", credentials);
    }

    if (!authenticate(requestContext, credentials, SecurityContext.BASIC_AUTH)) {
      throw new WebApplicationException(unauthorizedHandler.buildResponse(prefix, realm));
    }
  }

  /**
   * Builder for {@link SplitCookieCredentialAuthFilter}.
   *
   * <p>An {@link Authenticator} must be provided during the building process.
   *
   * @param <P> the type of the principal
   */
  public static class Builder<P extends Principal>
      extends AuthFilter.AuthFilterBuilder<String, P, SplitCookieCredentialAuthFilter<P>> {

    @Override
    protected SplitCookieCredentialAuthFilter<P> newInstance() {
      return new SplitCookieCredentialAuthFilter<>();
    }
  }
}
