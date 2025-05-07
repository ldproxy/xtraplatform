/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.entities.domain.KeyPathAlias;
import de.ii.xtraplatform.entities.domain.ValueEncodingWithNesting;
import de.ii.xtraplatform.values.api.ValueEncodingJackson;
import io.dropwizard.util.DataSize;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ValueEncodingJacksonWithNesting<T> extends ValueEncodingJackson<T>
    implements ValueEncodingWithNesting<T> {

  public ValueEncodingJacksonWithNesting(
      Jackson jackson, DataSize maxYamlFileSize, boolean failOnUnknownProperties) {
    super(jackson, maxYamlFileSize, failOnUnknownProperties);

    getMapper(FORMAT.JSON)
        .registerModule(EntityDeserialization.DESERIALIZE_MERGEABLE_MAP_BUILDER_WRAPPER)
        .registerModule(EntityDeserialization.DESERIALIZE_API_BUILDINGBLOCK_MIGRATION);

    getMapper(FORMAT.YAML)
        .registerModule(EntityDeserialization.DESERIALIZE_MERGEABLE_MAP_BUILDER_WRAPPER)
        .registerModule(EntityDeserialization.DESERIALIZE_API_BUILDINGBLOCK_MIGRATION);
  }

  @Override
  public byte[] nestPayload(
      byte[] payload,
      String formatString,
      List<String> nestingPath,
      Optional<KeyPathAlias> keyPathAlias)
      throws IOException {
    if (nestingPath.isEmpty()) {
      return payload;
    }

    FORMAT format;
    try {
      format = FORMAT.fromString(formatString);
    } catch (Throwable e) {
      // LOGGER.error("Could not deserialize, format '{}' unknown.", formatString);
      return payload;
    }

    // TODO: .metadata.yml.swp leads to invisible error, should be ignored either silently or with
    // log message

    ObjectMapper mapper = getMapper(format);

    Map<String, Object> data =
        mapper.readValue(payload, new TypeReference<LinkedHashMap<String, Object>>() {});

    for (int i = nestingPath.size() - 1; i >= 0; i--) {
      if (i == nestingPath.size() - 1 && keyPathAlias.isPresent()) {
        data = keyPathAlias.get().wrapMap(data);
        continue;
      }

      String key = nestingPath.get(i);
      data = ImmutableMap.of(key, data);
    }
    return mapper.writeValueAsBytes(data);
  }
}
