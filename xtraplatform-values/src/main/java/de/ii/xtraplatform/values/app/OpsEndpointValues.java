/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.ops.domain.OpsEndpoint;
import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.ValueFactories;
import de.ii.xtraplatform.values.domain.ValueFactory;
import de.ii.xtraplatform.values.domain.ValueStore;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@AutoBind
public class OpsEndpointValues implements OpsEndpoint {
  private final ValueStore valueStore;
  private final ValueFactories valueFactories;
  private final ObjectMapper objectMapper;

  @Inject
  public OpsEndpointValues(ValueStore valueStore, ValueFactories valueFactories, Jackson jackson) {
    this.valueStore = valueStore;
    this.valueFactories = valueFactories;
    this.objectMapper = jackson.getDefaultObjectMapper();
  }

  @Override
  public String getEntrypoint() {
    return "values";
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getValues() throws JsonProcessingException {
    Map<String, List<Map<String, String>>> values =
        valueFactories.getTypes().stream()
            .sorted(Comparator.naturalOrder())
            .map(
                valueType -> {
                  ValueFactory valueFactory = valueFactories.get(valueType);
                  List<Map<String, String>> valueInfos =
                      valueStore.forType(valueFactory.valueClass()).identifiers().stream()
                          .sorted(Comparator.naturalOrder())
                          .map(this::getValueInfo)
                          .collect(Collectors.toList());

                  return Map.entry(valueFactory.type(), valueInfos);
                })
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    return Response.ok(objectMapper.writeValueAsString(values)).build();
  }

  private Map<String, String> getValueInfo(Identifier identifier) {
    return ImmutableMap.of("path", identifier.asPath());
  }
}
