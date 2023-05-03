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
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ObjectArrays;
import dagger.Lazy;
import de.ii.xtraplatform.auth.domain.Role;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.services.domain.ImmutableServiceStatus;
import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.services.domain.ServiceBackgroundTasks;
import de.ii.xtraplatform.services.domain.ServiceData;
import de.ii.xtraplatform.services.domain.ServiceStatus;
import de.ii.xtraplatform.services.domain.TaskStatus;
import de.ii.xtraplatform.store.domain.BlobStore;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.ValueEncoding;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaultsStore;
import de.ii.xtraplatform.store.domain.entities.EntityDataStore;
import de.ii.xtraplatform.store.domain.entities.EntityFactoriesImpl;
import de.ii.xtraplatform.store.domain.entities.EntityFactory;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.EntityState.STATE;
import de.ii.xtraplatform.store.domain.entities.EntityStoreDecorator;
import de.ii.xtraplatform.streams.domain.EventStream;
import de.ii.xtraplatform.streams.domain.Reactive;
import de.ii.xtraplatform.web.domain.Endpoint;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import java.io.ByteArrayInputStream;
import java.io.IOException;
// import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
@RolesAllowed({Role.Minimum.EDITOR})
@Path("/admin/services")
@Produces(MediaType.APPLICATION_JSON)
public class ServicesEndpoint implements Endpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServicesEndpoint.class);

  private final EntityDataStore<EntityData> entityRepository;
  private final EntityDataStore<ServiceData> serviceRepository;
  private final EntityRegistry entityRegistry;
  private final EntityFactoriesImpl entityFactories;
  private final EntityDataDefaultsStore defaultsStore;
  private final ServiceBackgroundTasks serviceBackgroundTasks;
  private final ObjectMapper objectMapper;
  private final List<Consumer<EntityStateEvent>> entityStateSubscriber;
  private final EventStream<EntityStateEvent> eventStream;

  private final BlobStore featuresStore;

  @Inject
  ServicesEndpoint(
      EntityDataStore<?> entityRepository,
      EntityRegistry entityRegistry,
      Lazy<Set<EntityFactory>> entityFactories,
      EntityDataDefaultsStore defaultsStore,
      ServiceBackgroundTasks serviceBackgroundTasks,
      Reactive reactive,
      BlobStore blobStore) {
    this.entityRepository = (EntityDataStore<EntityData>) entityRepository;
    this.serviceRepository = getServiceRepository(this.entityRepository);
    this.entityRegistry = entityRegistry;
    this.entityFactories = new EntityFactoriesImpl(entityFactories);
    this.defaultsStore = defaultsStore;
    this.serviceBackgroundTasks = serviceBackgroundTasks;
    this.objectMapper = entityRepository.getValueEncoding().getMapper(ValueEncoding.FORMAT.JSON);
    this.entityStateSubscriber = new ArrayList<>();
    this.eventStream = new EventStream<>(reactive.runner("sse", 1, 1024), "state");
    this.featuresStore = blobStore.with("features");

    // TODO: sse, see /_events below
    /*eventStream.foreach(
        event -> {
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("BROADCASTING {}", entityStateSubscriber.size());
          }
          entityStateSubscriber.forEach(subscriber -> subscriber.accept(event));
        });
    entityRegistry.addEntityStateListener(
        event -> eventStream.queue(ImmutableEntityStateEvent.builder().from(event).build()));*/
  }

  EntityDataStore<ServiceData> getServiceRepository(EntityDataStore<EntityData> entityRepository) {
    return new EntityStoreDecorator<EntityData, ServiceData>() {
      @Override
      public EntityDataStore<EntityData> getDecorated() {
        return entityRepository;
      }

      @Override
      public String[] transformPath(String... path) {
        return ObjectArrays.concat("services", path);
      }
    };
  }

  @GET
  @CacheControl(noCache = true)
  public List<ServiceStatus> getServices(
      @Parameter(in = ParameterIn.COOKIE, hidden = true) @Auth User user) {
    return serviceRepository.ids().stream()
        .map(this::getServiceStatus)
        // .sorted(Comparator.comparingLong(ServiceStatus::getCreatedAt).reversed())
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

      // LOGGER.debug("ADD SERVICE {}: {}", id, request);

      HashMap<String, String> cleanRequest = new HashMap<>(request);
      cleanRequest.remove("autoTypes");
      cleanRequest.remove("filecontent");

      Map<String, Object> autoProvider;

      java.nio.file.Path filePath = null;

      if (Objects.equals(request.get("featureProviderType"), "WFS")) {
        autoProvider =
            new ImmutableMap.Builder<String, Object>()
                .putAll(request)
                .put("auto", "true")
                .put("autoPersist", "true")
                .put("entityStorageVersion", "2")
                .put(
                    "connectionInfo",
                    new ImmutableMap.Builder<String, Object>()
                        .put("connectorType", "HTTP")
                        .put("uri", request.get("url"))
                        .put("user", Optional.ofNullable(request.get("user")))
                        .put("password", Optional.ofNullable(request.get("password")))
                        .build())
                .build();
      } else if (request.get("filename") != null && request.get("filecontent") != null) {
        try {
          ByteArrayInputStream decodedContent =
              new ByteArrayInputStream(Base64.getDecoder().decode(request.get("filecontent")));
          String fileName = request.get("filename");
          filePath = java.nio.file.Path.of(fileName);

          if (featuresStore.has(filePath)) {
            String extension = "";
            String name = "";

            int idxOfDot = fileName.lastIndexOf(".");
            extension = fileName.substring(idxOfDot + 1);
            name = fileName.substring(0, idxOfDot);
            int counter = 1;

            while (featuresStore.has(filePath)) {
              fileName = name + "_" + counter + "." + extension;
              counter++;
              filePath = java.nio.file.Path.of(fileName);
            }
            featuresStore.put(filePath, decodedContent);
          } else {
            featuresStore.put(filePath, decodedContent);
          }
        } catch (IOException e) {
          throw new InternalServerErrorException(e.getMessage());
        }
        autoProvider =
            new ImmutableMap.Builder<String, Object>()
                .putAll(cleanRequest)
                .put("auto", "true")
                .put("autoPersist", "true")
                .put(
                    "autoTypes",
                    Optional.ofNullable(request.get("autoTypes"))
                        .map(
                            schemas ->
                                Splitter.on(',').trimResults().omitEmptyStrings().split(schemas))
                        .orElse(ImmutableList.of()))
                .put("entityStorageVersion", "2")
                .put(
                    "connectionInfo",
                    new ImmutableMap.Builder<String, Object>()
                        .put("connectorType", "SLICK")
                        .put("database", filePath.toString())
                        .put("dialect", "GPKG")
                        .put(
                            "schemas",
                            Optional.ofNullable(request.get("schemas"))
                                .map(
                                    schemas ->
                                        Splitter.on(',')
                                            .trimResults()
                                            .omitEmptyStrings()
                                            .split(schemas))
                                .orElse(ImmutableList.of()))
                        .build())
                .build();
      } else {
        autoProvider =
            new ImmutableMap.Builder<String, Object>()
                .putAll(cleanRequest)
                .put("auto", "true")
                .put("autoPersist", "true")
                .put(
                    "autoTypes",
                    Optional.ofNullable(request.get("autoTypes"))
                        .map(
                            schemas ->
                                Splitter.on(',').trimResults().omitEmptyStrings().split(schemas))
                        .orElse(ImmutableList.of()))
                .put("entityStorageVersion", "2")
                .put(
                    "connectionInfo",
                    new ImmutableMap.Builder<String, Object>()
                        .put("connectorType", "SLICK")
                        .put("host", request.get("host"))
                        .put("database", request.get("database"))
                        .put("user", request.get("user"))
                        .put("password", request.get("password"))
                        .put(
                            "schemas",
                            Optional.ofNullable(request.get("schemas"))
                                .map(
                                    schemas ->
                                        Splitter.on(',')
                                            .trimResults()
                                            .omitEmptyStrings()
                                            .split(schemas))
                                .orElse(ImmutableList.of()))
                        .build())
                .build();
      }

      Identifier identifier = Identifier.from(id, "providers");
      Identifier identifier2 = Identifier.from(id, "services");

      // TODO: error notification in manager
      EntityData provider = entityRepository.fromMap(identifier, autoProvider);
      // EntityData service = null;

      // TODO: background task, while running return status on GET
      EntityData provider2 =
          entityFactories.get("providers", provider.getEntitySubType()).hydrateData(provider);

      EntityData provider3 = entityRepository.put(identifier, provider2).join();

      Map<String, Object> autoService =
          new ImmutableMap.Builder<String, Object>()
              .putAll(cleanRequest)
              .put("auto", "true")
              .put("autoPersist", "true")
              .put("entityStorageVersion", "2")
              .build();

      EntityData serviceData = entityRepository.fromMap(identifier2, autoService);

      // TODO: background task, while running return status on GET
      ServiceData service2 =
          (ServiceData)
              entityFactories
                  .get("services", serviceData.getEntitySubType())
                  .hydrateData(serviceData);

      ServiceData added = serviceRepository.put(id, service2).join();

      return Response.ok().entity(getServiceStatus(added)).build();

    } /* catch (InterruptedException | ExecutionException e) {
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
      throw new BadRequestException(
          Objects.nonNull(e.getCause()) ? e.getCause().getMessage() : e.getMessage());
    } finally {
      LogContext.remove(LogContext.CONTEXT.SERVICE);
    }
  }

  // TODO: integrate in manager
  // TODO: use JAX-RS Sse support when upgraded to Dropwizard 2.0
  @Path("/_events")
  @GET
  @Produces("text/event-stream")
  public void getServiceEvents(
      @Parameter(in = ParameterIn.COOKIE, hidden = true) @Auth User user,
      @Suspended final AsyncResponse asyncResponse,
      @Context final HttpServletRequest httpServletRequest) {

    // 3.
    final HttpServletResponse httpServletResponse =
        (HttpServletResponse) httpServletRequest.getAttribute("original.response");
    final ServletOutputStream out;

    // 4.
    httpServletResponse.setHeader("Content-Type", "text/event-stream");
    try {
      httpServletResponse.flushBuffer();
      out = httpServletResponse.getOutputStream();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }

    entityStateSubscriber.add(
        entityStateEvent -> {
          try {
            // 8.
            out.write("data:".getBytes());
            objectMapper.writeValue(out, entityStateEvent);
            out.write("\n\n".getBytes());
            out.flush();
          } catch (final IOException e) {
            // client gone
            try {
              asyncResponse.resume("");
            } catch (final RuntimeException re) {
              // ignore
            }
          }
        });
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
      dataWithoutDefaults =
          defaultsStore.subtractDefaults(
              identifier, serviceData.getEntitySubType(), serviceDataMap, ImmutableList.of());
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

      // LOGGER.debug("PATCH SERVICE {}: {}", id, request);

      ServiceData updated = serviceRepository.patch(id, request).get();

      return Response.ok().entity(objectMapper.writeValueAsString(updated)).build();
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

      // LOGGER.debug("DELETE SERVICE {}", id);

      serviceRepository.delete(id).join();

      entityRepository.delete(id, "providers").join();

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

    STATE state =
        entityRegistry.getEntityState("services", serviceData.getId()).orElse(STATE.LOADING);
    Optional<TaskStatus> currentTaskForService =
        serviceBackgroundTasks.getCurrentTaskForService(serviceData.getId());

    ImmutableServiceStatus.Builder serviceStatus =
        ImmutableServiceStatus.builder().from(serviceData).status(state);

    if (currentTaskForService.isPresent()) {
      serviceStatus
          .hasBackgroundTask(true)
          .hasProgress(true)
          .progress((int) Math.round(currentTaskForService.get().getProgress() * 100))
          .message(
              String.format(
                  "%s: %s",
                  currentTaskForService.get().getLabel(),
                  currentTaskForService.get().getStatusMessage()));
    } else if (state == STATE.LOADING) {
      serviceStatus.hasBackgroundTask(true).message("Initializing");
    } else if (state == STATE.RELOADING) {
      serviceStatus.hasBackgroundTask(true).message("Reloading");
    }

    return serviceStatus.build();
  }
}
