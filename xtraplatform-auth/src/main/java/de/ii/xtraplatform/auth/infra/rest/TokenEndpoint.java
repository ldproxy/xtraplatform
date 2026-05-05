/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.infra.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.auth.domain.ImmutableTokenResponse;
import de.ii.xtraplatform.auth.domain.SplitCookie;
import de.ii.xtraplatform.auth.domain.TokenHandler;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.auth.domain.UserAuthenticator;
import de.ii.xtraplatform.services.domain.ServicesContext;
import de.ii.xtraplatform.web.domain.Endpoint;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@Singleton
@AutoBind
@Path("/auth")
public class TokenEndpoint implements Endpoint {

  private static final int DEFAULT_EXPIRY = 2_592_000;
  private final UserAuthenticator authenticator;
  private final TokenHandler tokenGenerator;
  private final URI servicesUri;

  @SuppressWarnings("PMD.DataClass")
  public static class Credentials {
    @JsonProperty public String user;
    @JsonProperty public String password;
    @JsonProperty public int expiration = DEFAULT_EXPIRY;
    @JsonProperty public boolean rememberMe;
    @JsonProperty public boolean noCookie;
  }

  @Inject
  public TokenEndpoint(
      UserAuthenticator authenticator,
      TokenHandler tokenGenerator,
      ServicesContext servicesContext) {
    this.authenticator = authenticator;
    this.tokenGenerator = tokenGenerator;
    this.servicesUri = servicesContext.getUri();
  }

  @RequestBody(
      required = true,
      content =
          @Content(
              examples = {
                @ExampleObject("{\"user\": \"admin\", \"password\": \"admin\", \"noCookie\": true}")
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
    .orElse(DEFAULT_EXPIRY)*/

    boolean rememberMe = body.rememberMe;
    // Boolean.parseBoolean(body.get("rememberMe"))

    String token = tokenGenerator.generateToken(user.get(), expiresIn, rememberMe);

    Response.ResponseBuilder response =
        Response.ok()
            .entity(
                ImmutableTokenResponse.builder().accessToken(token).expiresIn(expiresIn).build());

    if (!body.noCookie) {
      String domain = null; // request.getServerName();

      List<String> authCookies = SplitCookie.writeToken(token, domain, isSecure(), rememberMe);

      authCookies.forEach(cookie -> response.header("Set-Cookie", cookie));
    }

    return response.build();
  }

  @SuppressWarnings("PMD.UnusedPrivateMethod")
  private String getDomain() {
    return getExternalUri().getHost();
  }

  private boolean isSecure() {
    return false;
    // return Objects.equals(getExternalUri().getScheme(), "https");
  }

  private URI getExternalUri() {
    return servicesUri;
  }
}
