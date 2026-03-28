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
import de.ii.xtraplatform.web.domain.ForwardedUri;
import de.ii.xtraplatform.web.domain.LoginHandler;
import de.ii.xtraplatform.web.domain.MediaTypeCharset;
import de.ii.xtraplatform.web.domain.StaticResourceHandler;
import de.ii.xtraplatform.web.domain.URICustomizer;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.v3.oas.annotations.Hidden;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
@Path("/")
@Produces(MediaTypeCharset.APPLICATION_JSON_UTF8)
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
public class ServicesEndpoint implements Endpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServicesEndpoint.class);

  private final EntityRegistry entityRegistry;
  private final ServiceInjectableContext serviceContext;
  private final ServicesContext servicesContext;
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
    this.servicesContext = servicesContext;
    this.serviceContext = serviceContext;
    this.staticResourceHandler = staticResourceHandler;
    this.loginHandler = loginHandler;
    this.serviceResources = serviceResources;
    this.serviceListingProviders = serviceListingProviders;
  }

  // TODO
  @GET
  @Produces(MediaType.WILDCARD)
  @SuppressWarnings("PMD.UnusedFormalParameter") // callback parameter part of API contract
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

    MediaType mediaType = determineMediaType(f, containerRequestContext);

    Optional<ServiceListingProvider> provider =
        serviceListingProviders.get().stream()
            .filter(
                serviceListingProvider ->
                    Objects.equals(mediaType, serviceListingProvider.getMediaType()))
            .findFirst();

    if (provider.isPresent()) {
      Optional<Principal> user =
          Optional.ofNullable(containerRequestContext.getSecurityContext().getUserPrincipal());
      URICustomizer uriCustomizer =
          ForwardedUri.from(containerRequestContext, servicesContext)
              .clearParameters()
              .ensureNoTrailingSlash();
      Map<String, String> queryParameters =
          containerRequestContext.getUriInfo().getQueryParameters().entrySet().stream()
              .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));

      try (Response serviceListing =
          provider.get().getServiceListing(services, uriCustomizer, queryParameters, user)) {
        return Response.ok().entity(serviceListing.getEntity()).type(mediaType).build();
      }
    }

    return Response.ok().entity(services).build();
  }

  @GET
  @Path("/{service}" + StaticResourceHandler.PREFIX + "/{file: .+}")
  @Produces(MediaType.WILDCARD)
  public Response getAsset(
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
  @Path("/{service}/v{version}" + StaticResourceHandler.PREFIX + "/{file: .+}")
  @Produces(MediaType.WILDCARD)
  public Response getVersionedAsset(
      @PathParam("file") String file,
      @Context final HttpServletRequest request,
      @Context final HttpServletResponse response) {
    return getAsset(file, request, response);
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
            containerRequestContext,
            redirectUri,
            scopes,
            servicesContext.getUri().getPath(),
            false,
            null,
            null);
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
            containerRequestContext,
            redirectUri,
            null,
            servicesContext.getUri().getPath(),
            true,
            state,
            token);
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
  @SuppressWarnings("PMD.UnusedFormalParameter") // callback parameter part of API contract
  public ServiceEndpoint getServiceResource(
      @PathParam("service") String id,
      @QueryParam("callback") String callback,
      @Context ContainerRequestContext containerRequestContext) {
    return getVersionedServiceResource(id, callback, containerRequestContext, null);
  }

  @Path("/{service}/v{version}/")
  @SuppressWarnings("PMD.UnusedFormalParameter") // callback parameter part of API contract
  public ServiceEndpoint getVersionedServiceResource(
      @PathParam("service") String id,
      @QueryParam("callback") String callback,
      @Context ContainerRequestContext containerRequestContext,
      @PathParam("version") Integer version) {

    Service service = getService(id);

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
          redirectPath = getExternalUri().get().getPath() + redirectPath;
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

  private Service getService(String id) {
    Optional<Service> s = entityRegistry.getEntity(Service.class, id);

    if (s.isEmpty() /*|| s.get().getData().hasError()*/) {
      throw new NotFoundException();
    }

    return s.get();
  }

  private Optional<URI> getExternalUri() {
    return Optional.of(servicesContext.getUri());
  }

  private MediaType determineMediaType(String f, ContainerRequestContext containerRequestContext) {
    if (Objects.equals(f, "json")) {
      return MediaType.APPLICATION_JSON_TYPE;
    }

    if (Objects.equals(f, "html")) {
      return MediaType.TEXT_HTML_TYPE;
    }

    if (Objects.nonNull(containerRequestContext.getMediaType())) {
      return containerRequestContext.getMediaType();
    }

    List<MediaType> acceptableTypes = containerRequestContext.getAcceptableMediaTypes();
    if (!acceptableTypes.isEmpty() && !acceptableTypes.get(0).equals(MediaType.WILDCARD_TYPE)) {
      return acceptableTypes.get(0);
    }

    String userAgent = containerRequestContext.getHeaderString("user-agent");
    if (Objects.nonNull(userAgent)
        && userAgent.toLowerCase(Locale.ROOT).contains("google-site-verification")) {
      return MediaType.TEXT_HTML_TYPE;
    }

    return MediaType.APPLICATION_JSON_TYPE;
  }

  private void openLoggingContext(ContainerRequestContext containerRequestContext) {
    openLoggingContext(null, null, containerRequestContext);
  }

  private void openLoggingContext(
      String serviceId, Integer version, ContainerRequestContext containerRequestContext) {
    setupServiceContext(serviceId);
    setupRequestLogging(serviceId, version, containerRequestContext);
    logRequestUser(containerRequestContext);
    logRequestHeaders(containerRequestContext);
    logRequestBody(containerRequestContext);
  }

  private void setupServiceContext(String serviceId) {
    if (Objects.nonNull(serviceId)) {
      LogContext.put(LogContext.CONTEXT.SERVICE, serviceId);
    } else {
      LogContext.remove(LogContext.CONTEXT.SERVICE);
    }
  }

  private void setupRequestLogging(
      String serviceId, Integer version, ContainerRequestContext containerRequestContext) {
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
  }

  @SuppressWarnings("PMD.GuardLogStatement")
  private void logRequestUser(ContainerRequestContext containerRequestContext) {
    if (LOGGER.isDebugEnabled(MARKER.REQUEST_USER)) {
      Principal principal = containerRequestContext.getSecurityContext().getUserPrincipal();

      if (Objects.nonNull(principal)) {
        LOGGER.debug(MARKER.REQUEST_USER, "Request user: {}", principal.getName());
      } else {
        LOGGER.debug(MARKER.REQUEST_USER, "Request user: null");
      }
    }
  }

  private void logRequestHeaders(ContainerRequestContext containerRequestContext) {
    if (LOGGER.isDebugEnabled(MARKER.REQUEST_HEADER)) {
      String headers =
          Joiner.on("\n  ").withKeyValueSeparator(": ").join(containerRequestContext.getHeaders());

      LOGGER.debug(MARKER.REQUEST_HEADER, "Request headers: \n  {}", headers);
    }
  }

  private void logRequestBody(ContainerRequestContext containerRequestContext) {
    if (LOGGER.isDebugEnabled(MARKER.REQUEST_BODY) && containerRequestContext.hasEntity()) {
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
