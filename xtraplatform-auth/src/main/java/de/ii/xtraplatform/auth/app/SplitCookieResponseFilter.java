/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.auth.app.User.UserData;
import de.ii.xtraplatform.auth.domain.SplitCookie;
import de.ii.xtraplatform.auth.domain.TokenHandler;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.EntityDataStore;
import de.ii.xtraplatform.services.domain.ServicesContext;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class SplitCookieResponseFilter implements ContainerResponseFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(SplitCookieResponseFilter.class);

  private final URI servicesUri;
  private final TokenHandler tokenHandler;
  private final EntityDataStore<UserData> userRepository;

  @Inject
  public SplitCookieResponseFilter(
      ServicesContext servicesContext,
      TokenHandler tokenHandler,
      EntityDataStore<?> entityRepository) {
    this.servicesUri = servicesContext.getUri();
    this.tokenHandler = tokenHandler;
    this.userRepository =
        ((EntityDataStore<EntityData>) entityRepository)
            .forType(de.ii.xtraplatform.auth.app.User.UserData.class);
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

      // if forceChangePassword was enabled but userData no longer contains passwordExpiresAt,
      // disable forceChangePassword
      if (forceChangePassword) {
        Optional<User> user = tokenHandler.parseToken(token.get());
        Optional<UserData> userData =
            user.flatMap(user1 -> Optional.ofNullable(userRepository.get(user1.getName())));

        boolean disableForceChangePassword =
            !userData.map(user1 -> user1.getPasswordExpiresAt().isPresent()).orElse(false);

        if (user.isPresent() && disableForceChangePassword) {
          Optional<Date> exp = tokenHandler.parseTokenClaim(token.get(), "exp", Date.class);
          token =
              Optional.of(
                  exp.isPresent()
                      ? tokenHandler.generateToken(user.get(), exp.get(), rememberMe)
                      : tokenHandler.generateToken(user.get(), 60, rememberMe));
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
    return servicesUri;
  }
}
