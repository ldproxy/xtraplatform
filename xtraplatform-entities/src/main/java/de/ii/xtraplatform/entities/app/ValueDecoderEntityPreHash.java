/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.EntityDataBuilder;
import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.ValueDecoderMiddleware;
import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueDecoderEntityPreHash implements ValueDecoderMiddleware<EntityData> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ValueDecoderEntityPreHash.class);

  private final BiFunction<Identifier, String, EntityDataBuilder<EntityData>> newBuilderSupplier;
  private final Function<EntityData, String> hasher;

  // TODO: shouldPreHash from Factory
  public ValueDecoderEntityPreHash(
      BiFunction<Identifier, String, EntityDataBuilder<EntityData>> newBuilderSupplier,
      Function<EntityData, String> hasher) {
    this.newBuilderSupplier = newBuilderSupplier;
    this.hasher = hasher;
  }

  @Override
  public EntityData process(
      Identifier identifier,
      byte[] payload,
      ObjectMapper objectMapper,
      EntityData data,
      boolean ignoreCache)
      throws IOException {
    if (data.getEntitySubType().isPresent()) {
      String hash = hasher.apply(data);

      EntityDataBuilder<EntityData> builder =
          newBuilderSupplier.apply(identifier, data.getEntitySubType().get());

      // TODO: happens because providers declare subtypes despite not having any
      // no builder found for subtype
      if (builder == null) {
        return data;
      }

      builder.from(data);

      builder.stableHash(hash);

      // LOGGER.debug("PROC {} {}", identifier, hash);

      return builder.build();
    }

    return data;
  }
}
