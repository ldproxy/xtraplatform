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
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.auth.domain.Role;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.dropwizard.domain.Endpoint;
import de.ii.xtraplatform.services.domain.ServiceBackgroundTasks;
import de.ii.xtraplatform.store.domain.ValueEncoding;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataStore;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
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

@Component
@Provides
@Instantiate
@RolesAllowed({Role.Minimum.EDITOR})
@Path("/admin/entities")
@Produces(MediaType.APPLICATION_JSON)
public class EntitiesEndpoint implements Endpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(EntitiesEndpoint.class);

  private final EntityDataStore<EntityData> serviceRepository;
  private final EntityRegistry entityRegistry;
  private final ServiceBackgroundTasks serviceBackgroundTasks;
  private final ObjectMapper objectMapper;

  EntitiesEndpoint(
      @Requires EntityDataStore<EntityData> entityRepository,
      @Requires EntityRegistry entityRegistry
      /*@Requires ServiceBackgroundTasks serviceBackgroundTasks,*/ ) {
    this.serviceRepository = entityRepository;
    this.entityRegistry = entityRegistry;
    this.serviceBackgroundTasks = null; // serviceBackgroundTasks;
    this.objectMapper = entityRepository.getValueEncoding().getMapper(ValueEncoding.FORMAT.JSON);
  }

  @Path("/{entityType}")
  @GET
  @CacheControl(noCache = true)
  public Response getEntities(
      @Parameter(in = ParameterIn.COOKIE, hidden = true) @Auth User user,
      @PathParam("entityType") String entityType) {
    List<Map<String, String>> entities =
        serviceRepository.ids(entityType).stream()
            .map(id -> ImmutableMap.of("id", id))
            .collect(Collectors.toList());

    try {
      return Response.ok().entity(objectMapper.writeValueAsString(entities)).build();
    } catch (JsonProcessingException e) {
      throw new InternalServerErrorException();
    }
  }

  @Path("/{entityType}/{id}")
  @GET
  @CacheControl(noCache = true)
  public Response getEntity(
      @Parameter(in = ParameterIn.COOKIE, hidden = true) @Auth User user,
      @PathParam("entityType") String entityType,
      @PathParam("id") String id) {
    Optional<EntityData> entityData = Optional.ofNullable(serviceRepository.get(id, entityType));

    if (!entityData.isPresent()) {
      throw new NotFoundException();
    }

    try {
      return Response.ok().entity(objectMapper.writeValueAsString(entityData.get())).build();
    } catch (JsonProcessingException e) {
      throw new InternalServerErrorException();
    }
  }
}
