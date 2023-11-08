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
import de.ii.xtraplatform.values.domain.ValueCache;
import de.ii.xtraplatform.values.domain.ValueDecoderMiddleware;
import java.io.IOException;
import java.util.function.Function;

public class ValueDecoderWithBuilder<T extends StoredValue> implements ValueDecoderMiddleware<T> {

  private final Function<Identifier, ValueBuilder<T>> newBuilderSupplier;
  private final ValueCache<T> valueCache;

  public ValueDecoderWithBuilder(
      Function<Identifier, ValueBuilder<T>> newBuilderSupplier, ValueCache<T> valueCache) {
    this.newBuilderSupplier = newBuilderSupplier;
    this.valueCache = valueCache;
  }

  @Override
  public T process(
      Identifier identifier, byte[] payload, ObjectMapper objectMapper, T data, boolean ignoreCache)
      throws IOException {
    ValueBuilder<T> builder = newBuilderSupplier.apply(identifier);

    if (valueCache.has(identifier) && !ignoreCache) {
      builder.from(valueCache.get(identifier));
    }

    objectMapper.readerForUpdating(builder).readValue(payload);

    return builder.build();
  }
}
