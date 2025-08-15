/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.StoredValue;
import de.ii.xtraplatform.values.domain.ValueBuilder;
import de.ii.xtraplatform.values.domain.ValueDecoderMiddleware;
import java.io.IOException;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueDecoderPreHash<T extends StoredValue> implements ValueDecoderMiddleware<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ValueDecoderPreHash.class);

  private final Function<Identifier, ValueBuilder<T>> newBuilderSupplier;
  private final Function<T, String> hasher;

  // TODO: shouldPreHash from Factory
  public ValueDecoderPreHash(
      Function<Identifier, ValueBuilder<T>> newBuilderSupplier, Function<T, String> hasher) {
    this.newBuilderSupplier = newBuilderSupplier;
    this.hasher = hasher;
  }

  @Override
  public T process(
      Identifier identifier, byte[] payload, ObjectMapper objectMapper, T data, boolean ignoreCache)
      throws IOException {
    String hash = hasher.apply(data);

    ValueBuilder<T> builder = newBuilderSupplier.apply(identifier);

    builder.from(data);

    builder.stableHash(hash);

    // LOGGER.debug("PROC {} {}", identifier, hash);

    return builder.build();
  }
}
