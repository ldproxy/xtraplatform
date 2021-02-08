/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.infra.rest;

import de.ii.xtraplatform.auth.app.SplitCookie;
import de.ii.xtraplatform.auth.domain.ImmutableTokenResponse;
import de.ii.xtraplatform.auth.domain.ImmutableUser;
import de.ii.xtraplatform.auth.domain.Role;
import de.ii.xtraplatform.auth.domain.TokenHandler;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.auth.domain.UserAuthenticator;
import de.ii.xtraplatform.dropwizard.domain.Endpoint;
import de.ii.xtraplatform.dropwizard.domain.XtraPlatform;
import de.ii.xtraplatform.runtime.domain.AuthConfig;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides
@Instantiate
@Path("/auth")
public class TokenEndpoint implements Endpoint {

  private static final int DEFAULT_EXPIRY = 2592000;

  private final UserAuthenticator authenticator;
  private final TokenHandler tokenGenerator;
  private final XtraPlatform xtraPlatform;
  private final AuthConfig authConfig;

  public TokenEndpoint(
      @Requires UserAuthenticator authenticator,
      @Requires TokenHandler tokenGenerator,
      @Requires XtraPlatform xtraPlatform) {
    this.authenticator = authenticator;
    this.tokenGenerator = tokenGenerator;
    this.xtraPlatform = xtraPlatform;
    this.authConfig = xtraPlatform.getConfiguration().auth;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/token")
  public Response authorize(@Context HttpServletRequest request, Map<String, String> body)
      throws IOException {

    Optional<User> user = authenticator.authenticate(body.get("user"), body.get("password"));

    if (!user.isPresent()) {
      return Response.status(Status.BAD_REQUEST).entity("userOrPasswordInvalid").build();
    }

    int expiresIn =
        Optional.ofNullable(body.get("expiration"))
            .map(
                exp -> {
                  try {
                    return Integer.parseInt(exp);
                  } catch (NumberFormatException e) {
                    // so we use our default
                  }
                  return null;
                })
            .orElse(DEFAULT_EXPIRY);

    boolean rememberMe = Boolean.parseBoolean(body.get("rememberMe"));

    String token = tokenGenerator.generateToken(user.get(), expiresIn, rememberMe);

    Response.ResponseBuilder response =
        Response.ok()
            .entity(
                ImmutableTokenResponse.builder().access_token(token).expires_in(expiresIn).build());

    String domain = null; // request.getServerName();

    List<String> authCookies = SplitCookie.writeToken(token, domain, isSecure(), rememberMe);

    authCookies.forEach(cookie -> response.header("Set-Cookie", cookie));

    return response.build();
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
