/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.infra.rest;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.auth.app.SplitCookie;
import de.ii.xtraplatform.auth.domain.ImmutableTokenResponse;
import de.ii.xtraplatform.auth.domain.TokenHandler;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.auth.domain.UserAuthenticator;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AuthConfig;
import de.ii.xtraplatform.web.domain.Endpoint;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Singleton
@AutoBind
@Path("/auth")
public class TokenEndpoint implements Endpoint {

  private static final int DEFAULT_EXPIRY = 2592000;

  public static class Credentials {
    public String user;
    public String password;
    public int expiration = DEFAULT_EXPIRY;
    public boolean rememberMe = false;
    public boolean noCookie = false;
  }

  private final UserAuthenticator authenticator;
  private final TokenHandler tokenGenerator;
  private final AppContext appContext;
  private final AuthConfig authConfig;

  @Inject
  public TokenEndpoint(
      UserAuthenticator authenticator, TokenHandler tokenGenerator, AppContext appContext) {
    this.authenticator = authenticator;
    this.tokenGenerator = tokenGenerator;
    this.appContext = appContext;
    this.authConfig = appContext.getConfiguration().auth;
  }

  @RequestBody(
      required = true,
      content =
          @Content(
              examples = {
                @ExampleObject(
                    value = "{\"user\": \"admin\", \"password\": \"admin\", \"noCookie\": true}")
              },
              schema = @Schema(implementation = Credentials.class)))
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/token")
  public Response authorize(@Context HttpServletRequest request, Credentials body)
      throws IOException {

    Optional<User> user = authenticator.authenticate(body.user, body.password);

    if (!user.isPresent()) {
      return Response.status(Status.BAD_REQUEST).entity("userOrPasswordInvalid").build();
    }

    int expiresIn = body.expiration;
    /*Optional.ofNullable(body.get("expiration"))
    .map(
        exp -> {
          try {
            return Integer.parseInt(exp);
          } catch (NumberFormatException e) {
            // so we use our default
          }
          return null;
        })
    .orElse(DEFAULT_EXPIRY)*/ ;

    boolean rememberMe = body.rememberMe;
    ; // Boolean.parseBoolean(body.get("rememberMe"));

    String token = tokenGenerator.generateToken(user.get(), expiresIn, rememberMe);

    Response.ResponseBuilder response =
        Response.ok()
            .entity(
                ImmutableTokenResponse.builder().access_token(token).expires_in(expiresIn).build());

    if (!body.noCookie) {
      String domain = null; // request.getServerName();

      List<String> authCookies = SplitCookie.writeToken(token, domain, isSecure(), rememberMe);

      authCookies.forEach(cookie -> response.header("Set-Cookie", cookie));
    }

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

  // TODO: from ServicesContext
  private URI getExternalUri() {
    return appContext.getUri().resolve("rest/services");
  }
}
