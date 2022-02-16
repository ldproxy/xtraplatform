/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.sun.source.doctree.SeeTree;
import dagger.Lazy;
import de.ii.xtraplatform.web.domain.Dropwizard;
import de.ii.xtraplatform.web.domain.Endpoint;
import de.ii.xtraplatform.web.domain.MediaTypeCharset;
import de.ii.xtraplatform.web.domain.StaticResourceHandler;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.services.domain.ServiceData;
import de.ii.xtraplatform.services.domain.ServiceEndpoint;
import de.ii.xtraplatform.services.domain.ServiceInjectableContext;
import de.ii.xtraplatform.services.domain.ServiceListingProvider;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.v3.oas.annotations.Hidden;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
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

/** @author zahnen */
@Singleton
@AutoBind
@Hidden
@Path("/services/")
@Produces(MediaTypeCharset.APPLICATION_JSON_UTF8)
public class ServicesEndpoint implements Endpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServicesEndpoint.class);

  private final EntityRegistry entityRegistry;
  private final ServiceInjectableContext serviceContext;
  private final AppContext appContext;
  private final StaticResourceHandler staticResourceHandler;

  private Lazy<Set<ServiceEndpoint>> serviceResources;
  private Lazy<Set<ServiceListingProvider>> serviceListingProviders;

  @Inject
  public ServicesEndpoint(
      EntityRegistry entityRegistry,
      AppContext appContext,
      ServiceInjectableContext serviceContext,
      StaticResourceHandler staticResourceHandler,
      Lazy<Set<ServiceEndpoint>> serviceResources,
      Lazy<Set<ServiceListingProvider>> serviceListingProviders) {
    this.entityRegistry = entityRegistry;
    this.appContext = appContext;
    this.serviceContext = serviceContext;
    this.staticResourceHandler = staticResourceHandler;
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
            .filter(serviceData -> !serviceData.hasError())
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

    Optional<ServiceListingProvider> provider = serviceListingProviders.get()
        .stream()
        .filter(serviceListingProvider -> Objects.equals(mediaType,
            serviceListingProvider.getMediaType()))
        .findFirst();

    if (provider.isPresent()) {
      Response serviceListing = provider.get().getServiceListing(services, uriInfo.getRequestUri());
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
    return serviceResources.get()
        .stream()
        .filter(serviceEndpoint -> Objects.equals(s.getServiceType(), serviceEndpoint.getServiceType()))
        .findFirst()
        .orElseThrow();
  }

  private Service getService(String id, String callback) {
    Optional<Service> s = entityRegistry.getEntity(Service.class, id);

    if (!s.isPresent() || s.get().getData().hasError()) {
      throw new NotFoundException();
    }

    return s.get();
  }

  //TODO: to ServicesContext
  private Optional<URI> getExternalUri() {
    return Optional.of(appContext.getUri().resolve("rest/services"));
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

    if (LOGGER.isDebugEnabled()) {
      LogContext.put(LogContext.CONTEXT.REQUEST, LogContext.generateRandomUuid().toString());
      LOGGER.debug(
          "Processing request: {} {}",
          containerRequestContext.getMethod(),
          formatUri(containerRequestContext.getUriInfo().getRequestUri(), serviceId, version));
    } else {
      LogContext.remove(LogContext.CONTEXT.REQUEST);
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
