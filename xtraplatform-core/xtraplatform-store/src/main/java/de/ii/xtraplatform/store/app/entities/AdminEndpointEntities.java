/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app.entities;

import static de.ii.xtraplatform.store.domain.entities.EntityDataStore.entityType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.entities.EntityDataStore;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.EntityState;
import de.ii.xtraplatform.store.domain.entities.EntityState.STATE;
import de.ii.xtraplatform.web.domain.AdminSubEndpoint;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class AdminEndpointEntities implements AdminSubEndpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(AdminEndpointEntities.class);

  private final HttpServlet servlet;
  private final EntityDataStore<?> entityDataStore;
  private final EntityRegistry entityRegistry;
  private final ObjectMapper objectMapper;

  @Inject
  public AdminEndpointEntities(
      EntityDataStore<?> entityDataStore, EntityRegistry entityRegistry, Jackson jackson) {
    this.entityDataStore = entityDataStore;
    this.entityRegistry = entityRegistry;
    this.objectMapper = jackson.getDefaultObjectMapper();
    this.servlet = new EntitiesServlet();
  }

  @Override
  public String getPath() {
    return "/entities";
  }

  @Override
  public HttpServlet getServlet() {
    return servlet;
  }

  class EntitiesServlet extends HttpServlet {
    private static final long serialVersionUID = 3772654177231086757L;
    private static final String CONTENT_TYPE = "application/json";
    private static final String CACHE_CONTROL = "Cache-Control";
    private static final String NO_CACHE = "must-revalidate,no-cache,no-store";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setHeader(CACHE_CONTROL, NO_CACHE);
      resp.setContentType(CONTENT_TYPE);

      LinkedHashMap<String, List<Map<String, String>>> entities =
          entityDataStore.identifiers().stream()
              .sorted(Comparator.naturalOrder())
              .collect(
                  Collectors.groupingBy(
                      EntityDataStore::entityType,
                      LinkedHashMap::new,
                      Collectors.mapping(this::getEntityInfo, Collectors.toList())));

      try (PrintWriter writer = resp.getWriter()) {
        objectMapper.writeValue(writer, entities);
      }
    }

    private ImmutableMap<String, String> getEntityInfo(Identifier identifier) {
      Optional<EntityState.STATE> state =
          entityRegistry.getEntityState(entityType(identifier), identifier.id());
      return ImmutableMap.of("id", identifier.id(), "status", state.orElse(STATE.UNKNOWN).name());
    }
  }
}
