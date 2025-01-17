/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.app;

import static de.ii.xtraplatform.entities.domain.EntityDataStore.entityType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.entities.domain.EntityDataStore;
import de.ii.xtraplatform.entities.domain.EntityFactories;
import de.ii.xtraplatform.entities.domain.EntityFactory;
import de.ii.xtraplatform.entities.domain.EntityRegistry;
import de.ii.xtraplatform.entities.domain.EntityState;
import de.ii.xtraplatform.entities.domain.EntityState.STATE;
import de.ii.xtraplatform.ops.domain.OpsEndpoint;
import de.ii.xtraplatform.values.domain.Identifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api/entities")
@Singleton
@AutoBind
public class OpsEndpointEntities implements OpsEndpoint {
  private final EntityDataStore<?> entityDataStore;
  private final EntityRegistry entityRegistry;
  private final EntityFactories entityFactories;
  private final ObjectMapper objectMapper;

  @Inject
  public OpsEndpointEntities(
      EntityDataStore<?> entityDataStore,
      EntityRegistry entityRegistry,
      EntityFactories entityFactories,
      Jackson jackson) {
    this.entityDataStore = entityDataStore;
    this.entityRegistry = entityRegistry;
    this.entityFactories = entityFactories;
    this.objectMapper = jackson.getDefaultObjectMapper();
  }

  public class EntityResponse {
    public List<Entity> providers;
    public List<Entity> services;

    public static class Entity {
      public String id;
      public String status;
      public String subType;
    }
  }

  @Override
  public String getEntrypoint() {
    return "entities";
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get all entities", description = "Returns a list of all entities")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content =
                @Content(
                    mediaType = "application/json",
                    schema =
                        @Schema(
                            implementation = EntityResponse.class,
                            example =
                                "{\n  \"providers\" : [ {\n    \"id\" : \"testOpenApi\",\n    \"status\" : \"ACTIVE\",\n    \"subType\" : \"feature/wfs\"\n  } ],\n  \"services\" : [ {\n    \"id\" : \"testOpenApi\",\n    \"status\" : \"ACTIVE\",\n    \"subType\" : \"ogc_api\"\n  } ]\n}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public Response getEntities() throws JsonProcessingException {
    Map<String, List<Map<String, String>>> entities =
        entityFactories.getTypes().stream()
            .filter(entityType -> !Objects.equals(entityType, "users"))
            .sorted(Comparator.naturalOrder())
            .map(
                entityType -> {
                  EntityFactory entityFactory = entityFactories.get(entityType);
                  List<Map<String, String>> entityInfos =
                      entityDataStore.identifiers().stream()
                          .sorted(Comparator.naturalOrder())
                          .filter(identifier -> entityType.equals(entityType(identifier)))
                          .map(this::getEntityInfo)
                          .collect(Collectors.toList());

                  return Map.entry(entityFactory.type(), entityInfos);
                })
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    return Response.ok(objectMapper.writeValueAsString(entities)).build();
  }

  private ImmutableMap<String, String> getEntityInfo(Identifier identifier) {
    Optional<EntityState.STATE> state =
        entityRegistry.getEntityState(entityType(identifier), identifier.id());
    Optional<String> entitySubType = entityDataStore.get(identifier).getEntitySubType();
    return ImmutableMap.of(
        "id",
        identifier.id(),
        "status",
        state.orElse(STATE.UNKNOWN).name(),
        "subType",
        entitySubType.orElse(""));
  }
}
