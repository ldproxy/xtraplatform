/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xtraplatform.store.app.EventSourcing;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.ValueDecoderMiddleware;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import java.io.IOException;
import java.util.function.BiFunction;

public class ValueDecoderEntitySubtype implements ValueDecoderMiddleware<EntityData> {

  private final BiFunction<Identifier, String, EntityDataBuilder<EntityData>> newBuilderSupplier;
  private final EventSourcing<EntityData> eventSourcing; // TODO -> ValueCache

  public ValueDecoderEntitySubtype(
      BiFunction<Identifier, String, EntityDataBuilder<EntityData>> newBuilderSupplier,
      EventSourcing<EntityData> eventSourcing) {
    this.newBuilderSupplier = newBuilderSupplier;
    this.eventSourcing = eventSourcing;
  }

  @Override
  public EntityData process(
      Identifier identifier, byte[] payload, ObjectMapper objectMapper, EntityData data)
      throws IOException {
    if (data.getEntitySubType().isPresent()) {
      EntityDataBuilder<EntityData> builder =
          newBuilderSupplier.apply(identifier, data.getEntitySubType().get());

      //TODO: happens because providers declare subtypes despite not having any
      // no builder found for subtype
      if (builder == null) {
        return data;
      }

      if (eventSourcing.isInCache(identifier)) {
        builder.from(eventSourcing.getFromCache(identifier));
      }

      objectMapper.readerForUpdating(builder).readValue(payload);

      return builder.build();
    }

    return data;
  }
}
