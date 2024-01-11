/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.infra.rest;

import static de.ii.xtraplatform.services.domain.ServicesContext.STATIC_PREFIX;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.auth.domain.Oidc;
import de.ii.xtraplatform.auth.domain.SplitCookie;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AuthConfiguration;
import de.ii.xtraplatform.services.domain.ServicesContext;
import de.ii.xtraplatform.web.domain.Endpoint;
import de.ii.xtraplatform.web.domain.LoginHandler;
import de.ii.xtraplatform.web.domain.URICustomizer;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;

@Singleton
@AutoBind
public class OidcEndpoint implements Endpoint, LoginHandler {
  private final URI externalUriRoot;
  private final String servicesPath;
  private final AuthConfiguration authConfig;
  private final Oidc oidc;

  @Inject
  public OidcEndpoint(AppContext appContext, ServicesContext servicesContext, Oidc oidc) {
    this.externalUriRoot =
        URI.create(
            new URICustomizer()
                .setScheme(servicesContext.getUri().getScheme())
                .setHost(servicesContext.getUri().getHost())
                .setPort(servicesContext.getUri().getPort())
                .toString());
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

    return handle(containerRequestContext, redirectUri, scopes, "/", false, null, null);
  }

  @GET
  @Path(PATH_CALLBACK)
  @Produces(MediaType.TEXT_HTML)
  public Response getCallback(
      @QueryParam(LoginHandler.PARAM_CALLBACK_STATE) String state,
      @QueryParam(LoginHandler.PARAM_LOGIN_REDIRECT_URI) String redirectUri,
      @QueryParam(LoginHandler.PARAM_CALLBACK_TOKEN) String token,
      @Context ContainerRequestContext containerRequestContext) {

    return handle(containerRequestContext, redirectUri, null, "/", true, state, token);
  }

  @GET
  @Path(PATH_LOGOUT)
  @Produces(MediaType.TEXT_HTML)
  public Response getLogout(
      @QueryParam(LoginHandler.PARAM_LOGOUT_REDIRECT_URI) String redirectUri,
      @Context ContainerRequestContext containerRequestContext) {

    return logout(containerRequestContext, redirectUri);
  }

  private URI getCallbackUri(String rootPath) {
    String callbackPath = java.nio.file.Path.of(rootPath, "_callback").toString();
    return URI.create(new URICustomizer(externalUriRoot).setPath(callbackPath).toString());
  }

  private URI getCallbackRedirectUri(String rootPath, String redirectUri) {
    String callbackPath = java.nio.file.Path.of(rootPath, "_callback").toString();
    return URI.create(
        new URICustomizer(externalUriRoot)
            .setPath(callbackPath)
            .addParameter(LoginHandler.PARAM_LOGIN_REDIRECT_URI, redirectUri)
            .toString());
  }

  private String getStaticUrlPrefix() {
    return new URICustomizer(externalUriRoot)
        .setPath(servicesPath)
        .appendPath(STATIC_PREFIX)
        .getPath();
  }

  @Override
  public Response handle(
      ContainerRequestContext containerRequestContext,
      String redirectUri,
      String scopes,
      String rootPath,
      boolean isCallback,
      String state,
      String token) {
    URI callbackUri = getCallbackUri(rootPath);

    String redirect =
        isCallback && Objects.isNull(token) && Objects.nonNull(state)
            ? getCallbackRedirectUri(rootPath, state).toString()
            : redirectUri;

    String staticUrlPrefix = getStaticUrlPrefix();

    ResponseBuilder response =
        Response.ok(
            new OidcView(
                oidc.getConfigurationUri().replace("/.well-known/openid-configuration", ""),
                callbackUri.toString(),
                redirect,
                oidc.getClientId(),
                oidc.getClientSecret().orElse(null),
                scopes,
                state,
                token,
                isCallback,
                staticUrlPrefix));

    if (Objects.nonNull(token)) {
      List<String> authCookies =
          SplitCookie.writeToken(token, getDomain(callbackUri), isSecure(callbackUri), false);

      authCookies.forEach(cookie -> response.header("Set-Cookie", cookie));
    }

    return response.build();
  }

  @Override
  public Response logout(ContainerRequestContext containerRequestContext, String redirectUri) {
    URI logoutUri =
        Objects.nonNull(redirectUri)
            ? UriBuilder.fromUri(oidc.getLogoutUri())
                .queryParam(LoginHandler.PARAM_LOGOUT_CLIENT_ID, oidc.getClientId())
                .queryParam(LoginHandler.PARAM_LOGOUT_REDIRECT_URI, redirectUri)
                .build()
            : oidc.getLogoutUri();

    ResponseBuilder response = Response.seeOther(logoutUri);

    List<String> authCookies =
        SplitCookie.deleteToken(
            getDomain(containerRequestContext.getUriInfo().getRequestUri()),
            isSecure(containerRequestContext.getUriInfo().getRequestUri()));

    authCookies.forEach(cookie -> response.header("Set-Cookie", cookie));

    return response.build();
  }

  private String getDomain(URI uri) {
    return uri.getHost();
  }

  private boolean isSecure(URI uri) {
    return Objects.equals(uri.getScheme(), "https");
  }
}
