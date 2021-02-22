/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.ValueDecoderMiddleware;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import java.io.IOException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueDecoderIdValidator implements ValueDecoderMiddleware<EntityData> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ValueDecoderIdValidator.class);

  @Override
  public EntityData process(
      Identifier identifier,
      byte[] payload,
      ObjectMapper objectMapper,
      EntityData data,
      boolean ignoreCache)
      throws IOException {

    if (!Objects.equals(identifier.id(), data.getId())) {
      LOGGER.error(
          "Id mismatch: ignored entity '{}' because 'id' is set to '{}'",
          identifier.asPath(),
          data.getId());
      return null;
    }

    return data;
  }
}
