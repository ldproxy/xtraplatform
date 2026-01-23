/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.infra.rest;

import de.ii.xtraplatform.auth.app.ImmutableUserData;
import de.ii.xtraplatform.auth.app.PasswordHash;
import de.ii.xtraplatform.auth.app.User;
import de.ii.xtraplatform.auth.app.User.UserData;
import de.ii.xtraplatform.auth.domain.Role;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.EntityDataStore;
import de.ii.xtraplatform.web.domain.Endpoint;
import de.ii.xtraplatform.web.domain.MediaTypeCharset;
import io.dropwizard.auth.Auth;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
@Singleton
// @AutoBind
@RolesAllowed({Role.Minimum.ADMIN})
@Path("/admin/users")
@Produces(MediaTypeCharset.APPLICATION_JSON_UTF8)
public class UserAdminEndpoint implements Endpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserAdminEndpoint.class);

  private static final String FIELD_NEW_PASSWORD = "newPassword";
  private final EntityDataStore<UserData> userRepository;

  @Inject
  UserAdminEndpoint(EntityDataStore<?> entityRepository) {
    this.userRepository =
        ((EntityDataStore<EntityData>) entityRepository).forType(User.UserData.class);
  }

  @GET
  public List<String> getUsers(@Auth de.ii.xtraplatform.auth.domain.User user) {
    return userRepository.ids();
  }

  @GET
  @Path("/{id}")
  public User.UserData getUser(
      @Auth de.ii.xtraplatform.auth.domain.User user, @PathParam("id") String id) {
    return userRepository.get(id);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public User.UserData addUser(
      @Auth de.ii.xtraplatform.auth.domain.User user, Map<String, String> request) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Adding user {}", request);
    }

    if (userRepository.has(request.get("id"))) {
      throw new BadRequestException("User already exists.");
    }

    try {
      User.UserData userData =
          new ImmutableUserData.Builder()
              .id(request.get("id"))
              .password(PasswordHash.createHash(request.get("password")))
              .role(Role.fromString(request.get("role")))
              .build();

      CompletableFuture<User.UserData> put = userRepository.put(request.get("id"), userData);
      return put.get();

    } catch (IllegalStateException | InterruptedException | ExecutionException e) {
      LogContext.error(LOGGER, e, "Error adding user");
      throw new BadRequestException(e);
    }
  }

  @Path("/{id}")
  @POST
  @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.PreserveStackTrace"})
  public Response updateUser(
      @Auth de.ii.xtraplatform.auth.domain.User user,
      @PathParam("id") String id,
      Map<String, String> request) {

    if (!userRepository.has(id)) {
      throw new NotFoundException();
    }

    if (!request.containsKey("oldPassword")
        || !request.containsKey(FIELD_NEW_PASSWORD)
        || !request.containsKey("newPasswordRepeat")
        || !Objects.equals(request.get(FIELD_NEW_PASSWORD), request.get(FIELD_NEW_PASSWORD))) {
      throw new BadRequestException();
    }

    User.UserData userData = userRepository.get(id);

    if (!PasswordHash.validatePassword(request.get("oldPassword"), userData.getPassword())) {
      throw new BadRequestException();
    }

    User.UserData updated =
        new ImmutableUserData.Builder()
            .from(userData)
            .password(PasswordHash.createHash(request.get(FIELD_NEW_PASSWORD)))
            .passwordExpiresAt(OptionalLong.empty())
            .build();

    try {
      User.UserData updated2 = userRepository.put(id, updated).get();

      return Response.ok().entity(updated2).build();
    } catch (InterruptedException | ExecutionException e) {
      if (e.getCause() instanceof IllegalArgumentException) {
        throw new BadRequestException(e.getCause().getMessage());
      }
      throw new InternalServerErrorException(e);
    }
  }

  @DELETE
  @Path("/{id}")
  public void deleteUser(@Auth de.ii.xtraplatform.auth.domain.User user, @PathParam("id") String id)
      throws IOException {
    userRepository.delete(id);
  }
}
