/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app;

import de.ii.xtraplatform.auth.domain.TokenHandler;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.dropwizard.domain.XtraPlatform;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.SecurityContext;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Provides
@Instantiate
public class SplitCookieResponseFilter implements ContainerResponseFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(SplitCookieResponseFilter.class);

  private final XtraPlatform xtraPlatform;

  private final TokenHandler tokenHandler;

  public SplitCookieResponseFilter(
      @Requires XtraPlatform xtraPlatform, @Requires TokenHandler tokenHandler) {
    this.xtraPlatform = xtraPlatform;
    this.tokenHandler = tokenHandler;
  }

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {

    // TODO: would it be faster/easier to save needed information in context in
    // SplitCookieCredentialAuthFilter and only read it here?

    boolean isAuthenticated =
        Optional.ofNullable(requestContext.getSecurityContext())
            .map(SecurityContext::getUserPrincipal)
            .isPresent();

    int status = responseContext.getStatus();

    Optional<String> token = SplitCookie.readToken(requestContext.getCookies());

    // LOGGER.debug("RESPONSE {} {} {} {}", requestContext.getUriInfo().getRequestUri(), status,
    // isAuthenticated, token);

    if (status == 200 && isAuthenticated && token.isPresent()) {
      boolean forceChangePassword =
          tokenHandler
              .parseTokenClaim(token.get(), "forceChangePassword", Boolean.class)
              .orElse(false);
      boolean rememberMe =
          tokenHandler.parseTokenClaim(token.get(), "rememberMe", Boolean.class).orElse(false);

      if (forceChangePassword) {
        Optional<User> user = tokenHandler.parseToken(token.get());
        boolean toggleForceChangePassword = !user.map(User::getForceChangePassword).orElse(false);

        if (user.isPresent() && toggleForceChangePassword) {
          token = Optional.of(tokenHandler.generateToken(user.get(), 60, rememberMe));
        }
      }

      List<String> authCookies =
          SplitCookie.writeToken(token.get(), getDomain(), isSecure(), rememberMe);

      authCookies.forEach(cookie -> responseContext.getHeaders().add("Set-Cookie", cookie));
    }
  }

  // TODO: instead of external url, get request url
  // TODO: but we want to access view action links with same token, would that work?
  private String getDomain() {
    return getExternalUri().getHost();
  }

  // TODO: even if external url is set, we might want to access manager via http://localhost
  private boolean isSecure() {
    return false;
    // return Objects.equals(getExternalUri().getScheme(), "https");
  }

  private URI getExternalUri() {
    return xtraPlatform.getServicesUri();
  }
}