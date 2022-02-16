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
import de.ii.xtraplatform.auth.domain.Role;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.web.domain.Endpoint;
import de.ii.xtraplatform.services.domain.ServiceBackgroundTasks;
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
import java.util.Map;
import java.util.Optional;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
@RolesAllowed({Role.Minimum.EDITOR})
@Path("/admin/defaults")
@Produces(MediaType.APPLICATION_JSON)
public class DefaultsEndpoint implements Endpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultsEndpoint.class);

  private final EntityDataStore<EntityData> serviceRepository;
  private final EntityRegistry entityRegistry;
  private final EntityDataDefaultsStore defaultsStore;
  private final ServiceBackgroundTasks serviceBackgroundTasks;
  private final ObjectMapper objectMapper;

  @Inject
  DefaultsEndpoint(
      EntityDataStore<?> entityRepository,
      EntityRegistry entityRegistry,
      EntityDataDefaultsStore defaultsStore
      /*@Requires ServiceBackgroundTasks serviceBackgroundTasks,*/ ) {
    this.serviceRepository = (EntityDataStore<EntityData>) entityRepository;
    this.entityRegistry = entityRegistry;
    this.defaultsStore = defaultsStore;
    this.serviceBackgroundTasks = null; // serviceBackgroundTasks;
    this.objectMapper = entityRepository.getValueEncoding().getMapper(ValueEncoding.FORMAT.JSON);
  }

  @Path("/{entityType}/{subType}")
  @GET
  @CacheControl(noCache = true)
  public Response getEntity(
      @Parameter(in = ParameterIn.COOKIE, hidden = true) @Auth User user,
      @PathParam("entityType") String entityType,
      @PathParam("subType") String subType) {

    // TODO: subType
    Optional<Map<String, Object>> defaults =
        defaultsStore.getAllDefaults(
            Identifier.from(EntityDataDefaultsStore.EVENT_TYPE, entityType),
            Optional.ofNullable(subType));

    if (!defaults.isPresent()) {
      throw new NotFoundException();
    }

    try {
      return Response.ok().entity(objectMapper.writeValueAsString(defaults.get())).build();
    } catch (JsonProcessingException e) {
      throw new InternalServerErrorException();
    }
  }

  @Path("/{entityType}/{subType}")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateEntity(
      @Parameter(in = ParameterIn.COOKIE, hidden = true) @Auth User user,
      @PathParam("entityType") String entityType,
      @PathParam("subType") String subType,
      @RequestBody(
              required = true,
              content = {@Content()})
          Map<String, Object> request) {

    Map<String, Object> patch =
        defaultsStore
            .patch(EntityDataDefaultsStore.EVENT_TYPE, request, entityType, subType)
            .join();

    try {
      return Response.ok().entity(objectMapper.writeValueAsString(patch)).build();
    } catch (JsonProcessingException e) {
      throw new InternalServerErrorException();
    }
  }
}
