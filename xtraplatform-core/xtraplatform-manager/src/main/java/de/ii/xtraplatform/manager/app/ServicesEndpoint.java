/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.manager.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.auth.domain.Role;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.dropwizard.domain.Endpoint;
import de.ii.xtraplatform.runtime.domain.LogContext;
import de.ii.xtraplatform.services.domain.ImmutableServiceDataCommon;
import de.ii.xtraplatform.services.domain.ImmutableServiceStatus;
import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.services.domain.ServiceBackgroundTasks;
import de.ii.xtraplatform.services.domain.ServiceData;
import de.ii.xtraplatform.services.domain.ServiceStatus;
import de.ii.xtraplatform.services.domain.TaskStatus;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.ValueEncoding;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaultsStore;
import de.ii.xtraplatform.store.domain.entities.EntityDataStore;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Component
@Provides
@Instantiate
@RolesAllowed({Role.Minimum.EDITOR})
@Path("/admin/services")
@Produces(MediaType.APPLICATION_JSON)
public class ServicesEndpoint implements Endpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServicesEndpoint.class);

  private final EntityDataStore<ServiceData> serviceRepository;
  private final EntityRegistry entityRegistry;
  private final EntityDataDefaultsStore defaultsStore;
  private final ServiceBackgroundTasks serviceBackgroundTasks;
  private final ObjectMapper objectMapper;

  ServicesEndpoint(
      @Requires EntityDataStore<EntityData> entityRepository,
      @Requires EntityRegistry entityRegistry,
      @Requires EntityDataDefaultsStore defaultsStore
      /*@Requires ServiceBackgroundTasks serviceBackgroundTasks,*/) {
    this.serviceRepository = entityRepository.forType(ServiceData.class);
    this.entityRegistry = entityRegistry;
    this.defaultsStore = defaultsStore;
    this.serviceBackgroundTasks = null; // serviceBackgroundTasks;
    this.objectMapper = entityRepository.getValueEncoding().getMapper(ValueEncoding.FORMAT.JSON);
  }

  @GET
  @CacheControl(noCache = true)
  public List<ServiceStatus> getServices(
      @Parameter(in = ParameterIn.COOKIE, hidden = true) @Auth User user) {
    return serviceRepository.ids().stream()
        .map(this::getServiceStatus)
        .collect(Collectors.toList());
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response addService(
      @Parameter(in = ParameterIn.COOKIE, hidden = true) @Auth User user,
      @RequestBody Map<String, String> request) {

    if (!request.containsKey("id")) {
      throw new BadRequestException("No id given");
    }

    String id = request.get("id");

    if (serviceRepository.has(id)) {
      throw new BadRequestException("A service with id '" + id + "' already exists");
    }

    try {
      LogContext.put(LogContext.CONTEXT.SERVICE, id);

      LOGGER.debug("ADD SERVICE {}: {}", id, request);

      ServiceData added = new ImmutableServiceDataCommon.Builder().id(id)
          .label(Optional.ofNullable(request.get("label")).orElse(id))
          .serviceType(Optional.ofNullable(request.get("serviceType")).orElse("OGC_API")).build();

      // TODO: how to get ServiceData from POST body
      //ServiceData serviceData = null;

      //ServiceData added = serviceRepository.put(id, serviceData).get();

      return Response.ok().entity(getServiceStatus(added)).build();

    }/* catch (InterruptedException | ExecutionException e) {
      if (serviceRepository.has(id)) {
        try {
          serviceRepository.delete(id);
        } catch (Throwable e2) {
          // ignore
        }
      }

      throw new BadRequestException(e.getCause().getMessage());
      // throw new InternalServerErrorException(e.getCause());
    }*/ catch (Throwable e) {
      throw new BadRequestException(e.getCause().getMessage());
    } finally {
      LogContext.remove(LogContext.CONTEXT.SERVICE);
    }
  }

  @Path("/{id}")
  @GET
  @CacheControl(noCache = true)
  public Response getService(
      @Parameter(in = ParameterIn.COOKIE, hidden = true) @Auth User user,
      @PathParam("id") String id) {

    if (!serviceRepository.has(id)) {
      throw new BadRequestException();
    }

    ServiceData serviceData =
        entityRegistry
            .getEntity(Service.class, id)
            .map(Service::getData)
            .orElseGet(() -> serviceRepository.get(id));

    Map<String, Object> dataWithoutDefaults;
    try {
      Identifier identifier = Identifier.from(id, Service.TYPE);
      Map<String, Object> serviceDataMap = serviceRepository.asMap(identifier, serviceData);
      dataWithoutDefaults = defaultsStore
          .subtractDefaults(identifier, serviceData.getEntitySubType(), serviceDataMap,
              ImmutableList.of());
    } catch (IOException e) {
      throw new InternalServerErrorException();
    }

    try {
      return Response.ok().entity(objectMapper.writeValueAsString(dataWithoutDefaults)).build();
    } catch (JsonProcessingException e) {
      throw new InternalServerErrorException();
    }
  }

  @Path("/{id}/status")
  @GET
  @CacheControl(noCache = true)
  public ServiceStatus getServiceStatus(
      @Parameter(in = ParameterIn.COOKIE, hidden = true) @Auth User user,
      @PathParam("id") String id) {

    if (!serviceRepository.has(id)) {
      throw new NotFoundException();
    }

    return getServiceStatus(id);
  }

  @Path("/{id}")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateService(
      @Parameter(in = ParameterIn.COOKIE, hidden = true) @Auth User user,
      @PathParam("id") String id,
      @RequestBody(
          required = true,
          content = {@Content()})
          Map<String, Object> request) {

    if (!serviceRepository.has(id)) {
      throw new NotFoundException();
    }

    try {
      LogContext.put(LogContext.CONTEXT.SERVICE, id);

      LOGGER.debug("PATCH SERVICE {}: {}", id, request);

      return getService(user, id);
      //ServiceData updated = serviceRepository.patch(id, request).get();

      //return Response.ok().entity(objectMapper.writeValueAsString(updated)).build();
    } catch (Throwable e) {
      throw new BadRequestException("Invalid request body: " + e.getMessage());
    } finally {
      LogContext.remove(LogContext.CONTEXT.SERVICE);
    }
  }

  @Path("/{id}")
  @DELETE
  public Response deleteService(
      @Parameter(in = ParameterIn.COOKIE, hidden = true) @Auth User user,
      @PathParam("id") String id) {
    try {
      LogContext.put(LogContext.CONTEXT.SERVICE, id);

      LOGGER.debug("DELETE SERVICE {}", id);

      //serviceRepository.delete(id).get();

      return Response.noContent().build();
    } catch (Throwable e) {
      throw new InternalServerErrorException();
    } finally {
      LogContext.remove(LogContext.CONTEXT.SERVICE);
    }
  }

  private ServiceStatus getServiceStatus(String id) {
    ServiceData serviceData = serviceRepository.get(id);

    return getServiceStatus(serviceData);
  }

  private ServiceStatus getServiceStatus(ServiceData serviceData) {

    boolean started = entityRegistry.getEntity(Service.class, serviceData.getId()).isPresent();

    if (serviceData.hasError()) {
      started = false;
    }

    boolean loading = serviceData.isLoading();

    Optional<TaskStatus> currentTaskForService =
        Optional
            .empty(); // TODO serviceBackgroundTasks.getCurrentTaskForService(serviceData.getId());

    ImmutableServiceStatus.Builder serviceStatus =
        ImmutableServiceStatus.builder()
            .from(serviceData)
            .status(started ? ServiceStatus.STATUS.STARTED : ServiceStatus.STATUS.STOPPED);
    if (currentTaskForService.isPresent()) {
      serviceStatus
          .hasBackgroundTask(true)
          .progress((int) Math.round(currentTaskForService.get().getProgress() * 100))
          .message(
              String.format(
                  "%s: %s",
                  currentTaskForService.get().getLabel(),
                  currentTaskForService.get().getStatusMessage()));
    } else if (loading) {
      serviceStatus.hasBackgroundTask(true).message("Initializing");
    }

    return serviceStatus.build();
  }
}
