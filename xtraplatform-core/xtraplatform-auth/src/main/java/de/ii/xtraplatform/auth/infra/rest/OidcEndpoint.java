/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.infra.rest;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.auth.domain.Oidc;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AuthConfiguration;
import de.ii.xtraplatform.services.domain.ServicesContext;
import de.ii.xtraplatform.web.domain.Endpoint;
import de.ii.xtraplatform.web.domain.LoginHandler;
import java.net.URI;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Singleton
@AutoBind
public class OidcEndpoint implements Endpoint, LoginHandler {
  private final String servicesPath;
  private final AuthConfiguration authConfig;
  private final Oidc oidc;

  @Inject
  public OidcEndpoint(AppContext appContext, ServicesContext servicesContext, Oidc oidc) {
    this.servicesPath = servicesContext.getUri().getPath();
    this.authConfig = appContext.getConfiguration().getAuth();
    this.oidc = oidc;
  }

  @GET
  @Path(PATH_LOGIN)
  @Produces(MediaType.TEXT_HTML)
  public Response getLogin(
      @QueryParam(LoginHandler.PARAM_LOGIN_REDIRECT_URI) String redirectUri,
      @QueryParam(LoginHandler.PARAM_LOGIN_SCOPES) String scopes,
      @Context ContainerRequestContext containerRequestContext) {

    return handle(containerRequestContext, redirectUri, scopes, "/", false);
  }

  @GET
  @Path(PATH_CALLBACK)
  @Produces(MediaType.TEXT_HTML)
  public Response getCallback(
      @QueryParam(LoginHandler.PARAM_CALLBACK_STATE) String redirectUri,
      @Context ContainerRequestContext containerRequestContext) {

    return handle(containerRequestContext, redirectUri, null, "/", true);
  }

  // TODO: include oauth4webapi, oauth.generateRandomCodeVerifier()
  // TODO: pass token to all links, also in map clients? or use splitcookie (only works for frontend
  // I guess)?
  // TODO: xacml abac, aggregate values to list, geometries to ?

  private static URI getCallbackUri(
      ContainerRequestContext containerRequestContext, String rootPath) {
    String callbackPath = java.nio.file.Path.of(rootPath, "_callback").toString();
    return containerRequestContext
        .getUriInfo()
        .getRequestUriBuilder()
        .replacePath(callbackPath)
        .replaceQuery("")
        .build();
  }

  @Override
  public Response handle(
      ContainerRequestContext containerRequestContext,
      String redirectUri,
      String scopes,
      String rootPath,
      boolean isCallback) {
    if (Objects.isNull(redirectUri)) {
      throw new BadRequestException("no redirect_uri given");
    }

    URI callbackUri = getCallbackUri(containerRequestContext, rootPath);

    return Response.ok(
            new OidcView(
                oidc.getConfigurationUri().replace("/.well-known/openid-configuration", ""),
                callbackUri.toString(),
                redirectUri,
                oidc.getClientId(),
                oidc.getClientSecret().orElse(null),
                scopes,
                isCallback))
        .build();
  }
}
