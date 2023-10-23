/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Joiner;
import dagger.Lazy;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.entities.domain.EntityRegistry;
import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.services.domain.ServiceData;
import de.ii.xtraplatform.services.domain.ServiceEndpoint;
import de.ii.xtraplatform.services.domain.ServiceInjectableContext;
import de.ii.xtraplatform.services.domain.ServiceListingProvider;
import de.ii.xtraplatform.services.domain.ServicesContext;
import de.ii.xtraplatform.web.domain.Endpoint;
import de.ii.xtraplatform.web.domain.LoginHandler;
import de.ii.xtraplatform.web.domain.MediaTypeCharset;
import de.ii.xtraplatform.web.domain.StaticResourceHandler;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.v3.oas.annotations.Hidden;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
@Hidden
@Path("/services/")
@Produces(MediaTypeCharset.APPLICATION_JSON_UTF8)
public class ServicesEndpoint implements Endpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServicesEndpoint.class);

  private final EntityRegistry entityRegistry;
  private final ServiceInjectableContext serviceContext;
  private final URI servicesUri;
  private final StaticResourceHandler staticResourceHandler;
  private final Lazy<Set<LoginHandler>> loginHandler;

  private final Lazy<Set<ServiceEndpoint>> serviceResources;
  private final Lazy<Set<ServiceListingProvider>> serviceListingProviders;

  @Inject
  public ServicesEndpoint(
      EntityRegistry entityRegistry,
      ServicesContext servicesContext,
      ServiceInjectableContext serviceContext,
      StaticResourceHandler staticResourceHandler,
      Lazy<Set<LoginHandler>> loginHandler,
      Lazy<Set<ServiceEndpoint>> serviceResources,
      Lazy<Set<ServiceListingProvider>> serviceListingProviders) {
    this.entityRegistry = entityRegistry;
    this.servicesUri = servicesContext.getUri();
    this.serviceContext = serviceContext;
    this.staticResourceHandler = staticResourceHandler;
    this.loginHandler = loginHandler;
    this.serviceResources = serviceResources;
    this.serviceListingProviders = serviceListingProviders;
  }

  // TODO
  @GET
  @Produces(MediaType.WILDCARD)
  public Response getServices(
      @QueryParam("callback") String callback,
      @QueryParam("f") String f,
      @Context UriInfo uriInfo,
      @Context ContainerRequestContext containerRequestContext) {
    openLoggingContext(containerRequestContext);

    List<ServiceData> services =
        entityRegistry.getEntitiesForType(Service.class).stream()
            .map(Service::getData)
            // .filter(serviceData -> !serviceData.hasError())
            .collect(Collectors.toList());

    MediaType mediaType =
        Objects.equals(f, "json")
            ? MediaType.APPLICATION_JSON_TYPE
            : Objects.equals(f, "html")
                ? MediaType.TEXT_HTML_TYPE
                : Objects.nonNull(containerRequestContext.getMediaType())
                    ? containerRequestContext.getMediaType()
                    : (containerRequestContext.getAcceptableMediaTypes().size() > 0
                            && !containerRequestContext
                                .getAcceptableMediaTypes()
                                .get(0)
                                .equals(MediaType.WILDCARD_TYPE))
                        ? containerRequestContext.getAcceptableMediaTypes().get(0)
                        : (Objects.nonNull(containerRequestContext.getHeaderString("user-agent"))
                                && containerRequestContext
                                    .getHeaderString("user-agent")
                                    .toLowerCase()
                                    .contains("google-site-verification")
                            ? MediaType.TEXT_HTML_TYPE
                            : MediaType.APPLICATION_JSON_TYPE);

    Optional<ServiceListingProvider> provider =
        serviceListingProviders.get().stream()
            .filter(
                serviceListingProvider ->
                    Objects.equals(mediaType, serviceListingProvider.getMediaType()))
            .findFirst();

    if (provider.isPresent()) {
      Optional<Principal> user =
          Optional.ofNullable(containerRequestContext.getSecurityContext().getUserPrincipal());
      Response serviceListing =
          provider.get().getServiceListing(services, uriInfo.getRequestUri(), user);
      return Response.ok().entity(serviceListing.getEntity()).type(mediaType).build();
    }

    return Response.ok().entity(services).build();
  }

  @GET
  @Path("/___static___/{file: .+}")
  @Produces(MediaType.WILDCARD)
  @CacheControl(maxAge = 3600)
  public Response getFile(
      @PathParam("file") String file,
      @Context final HttpServletRequest request,
      @Context final HttpServletResponse response) {

    boolean handled = staticResourceHandler.handle(file, request, response);

    if (handled) {
      return Response.ok().build();
    }

    return Response.status(Response.Status.NOT_FOUND).build();
  }

  @GET
  @Path(LoginHandler.PATH_LOGIN)
  @Produces(MediaType.TEXT_HTML)
  @CacheControl(maxAge = 3600)
  public Response getLogin(
      @QueryParam(LoginHandler.PARAM_LOGIN_REDIRECT_URI) String redirectUri,
      @QueryParam(LoginHandler.PARAM_LOGIN_SCOPES) String scopes,
      @Context ContainerRequestContext containerRequestContext) {
    if (loginHandler.get().isEmpty()) {
      throw new NotFoundException();
    }

    return loginHandler
        .get()
        .iterator()
        .next()
        .handle(
            containerRequestContext, redirectUri, scopes, servicesUri.getPath(), false, null, null);
  }

  @GET
  @Path(LoginHandler.PATH_CALLBACK)
  @Produces(MediaType.TEXT_HTML)
  @CacheControl(maxAge = 3600)
  public Response getCallback(
      @QueryParam(LoginHandler.PARAM_CALLBACK_STATE) String state,
      @QueryParam(LoginHandler.PARAM_LOGIN_REDIRECT_URI) String redirectUri,
      @QueryParam(LoginHandler.PARAM_CALLBACK_TOKEN) String token,
      @Context ContainerRequestContext containerRequestContext) {
    if (loginHandler.get().isEmpty()) {
      throw new NotFoundException();
    }

    return loginHandler
        .get()
        .iterator()
        .next()
        .handle(
            containerRequestContext, redirectUri, null, servicesUri.getPath(), true, state, token);
  }

  @GET
  @Path(LoginHandler.PATH_LOGOUT)
  @Produces(MediaType.TEXT_HTML)
  public Response getLogout(
      @QueryParam(LoginHandler.PARAM_LOGOUT_REDIRECT_URI) String redirectUri,
      @Context ContainerRequestContext containerRequestContext) {
    if (loginHandler.get().isEmpty()) {
      throw new NotFoundException();
    }

    return loginHandler.get().iterator().next().logout(containerRequestContext, redirectUri);
  }

  @Path("/{service}/")
  public ServiceEndpoint getServiceResource(
      @PathParam("service") String id,
      @QueryParam("callback") String callback,
      @Context ContainerRequestContext containerRequestContext) {
    return getVersionedServiceResource(id, callback, containerRequestContext, null);
  }

  @Path("/{service}/v{version}/")
  public ServiceEndpoint getVersionedServiceResource(
      @PathParam("service") String id,
      @QueryParam("callback") String callback,
      @Context ContainerRequestContext containerRequestContext,
      @PathParam("version") Integer version) {

    Service service = getService(id, callback);

    if (service.getData().getApiVersion().isPresent()) {
      Integer apiVersion = service.getData().getApiVersion().get();

      if (Objects.isNull(version)) {
        String redirectPath =
            containerRequestContext
                .getUriInfo()
                .getAbsolutePath()
                .getPath()
                .replace(id, String.format("%s/v%d", id, apiVersion));
        if (getExternalUri().isPresent()) {
          redirectPath = redirectPath.replace("/rest/services", getExternalUri().get().getPath());
        }
        URI redirectUri =
            containerRequestContext
                .getUriInfo()
                .getRequestUriBuilder()
                .replacePath(redirectPath)
                .build();

        throw new WebApplicationException(Response.temporaryRedirect(redirectUri).build());
      } else if (!Objects.equals(apiVersion, version)) {
        throw new NotFoundException();
      }
    } else if (Objects.nonNull(version)) {
      throw new NotFoundException();
    }

    openLoggingContext(id, version, containerRequestContext);

    serviceContext.inject(containerRequestContext, service);

    return getServiceResource(service);
  }

  // TODO: after switch to jersey 2.x, use Resource.from and move instantiation to factory
  // TODO: cache resource object per service
  private ServiceEndpoint getServiceResource(Service s) {
    return serviceResources.get().stream()
        .filter(
            serviceEndpoint -> Objects.equals(s.getServiceType(), serviceEndpoint.getServiceType()))
        .findFirst()
        .orElseThrow();
  }

  private Service getService(String id, String callback) {
    Optional<Service> s = entityRegistry.getEntity(Service.class, id);

    if (s.isEmpty() /*|| s.get().getData().hasError()*/) {
      throw new NotFoundException();
    }

    return s.get();
  }

  private Optional<URI> getExternalUri() {
    return Optional.of(servicesUri);
  }

  private void openLoggingContext(ContainerRequestContext containerRequestContext) {
    openLoggingContext(null, null, containerRequestContext);
  }

  private void openLoggingContext(
      String serviceId, Integer version, ContainerRequestContext containerRequestContext) {
    if (Objects.nonNull(serviceId)) {
      LogContext.put(LogContext.CONTEXT.SERVICE, serviceId);
    } else {
      LogContext.remove(LogContext.CONTEXT.SERVICE);
    }

    if (LOGGER.isDebugEnabled() || LOGGER.isDebugEnabled(MARKER.REQUEST)) {
      LogContext.put(LogContext.CONTEXT.REQUEST, LogContext.generateRandomUuid().toString());

      LOGGER.debug(
          MARKER.REQUEST,
          "Processing request: {} {}",
          containerRequestContext.getMethod(),
          formatUri(containerRequestContext.getUriInfo().getRequestUri(), serviceId, version));
    } else {
      LogContext.remove(LogContext.CONTEXT.REQUEST);
    }

    if (LOGGER.isDebugEnabled(MARKER.REQUEST_USER)) {
      Principal principal = containerRequestContext.getSecurityContext().getUserPrincipal();

      if (Objects.nonNull(principal)) {
        LOGGER.debug(MARKER.REQUEST_USER, "Request user: {}", principal.getName());
      } else {
        LOGGER.debug(MARKER.REQUEST_USER, "Request user: null");
      }
    }

    if (LOGGER.isDebugEnabled(MARKER.REQUEST_HEADER)) {
      String headers =
          Joiner.on("\n  ").withKeyValueSeparator(": ").join(containerRequestContext.getHeaders());

      LOGGER.debug(MARKER.REQUEST_HEADER, "Request headers: \n  {}", headers);
    }

    if (LOGGER.isDebugEnabled(MARKER.REQUEST_BODY)) {
      if (containerRequestContext.hasEntity()) {
        try {
          containerRequestContext.getEntityStream().mark(Integer.MAX_VALUE);
          String body =
              new String(
                  containerRequestContext.getEntityStream().readAllBytes(), StandardCharsets.UTF_8);
          containerRequestContext.getEntityStream().reset();

          LOGGER.debug(MARKER.REQUEST_BODY, "Request body: \n  {}", body);
        } catch (IOException e) {
          // ignore
        }
      }
    }
  }

  private static String formatUri(URI uri, String serviceId, Integer version) {
    String path = uri.getPath();

    if (Objects.nonNull(serviceId)) {
      path = path.substring(path.indexOf(serviceId) + serviceId.length());
    }
    if (Objects.nonNull(version)) {
      String versionString = "v" + version;
      path = path.substring(path.indexOf(versionString) + versionString.length());
    }

    if (path.isEmpty()) {
      path = "/";
    }

    if (uri.getQuery() == null) {
      return path;
    }

    return path + "?" + uri.getQuery();
  }
}
