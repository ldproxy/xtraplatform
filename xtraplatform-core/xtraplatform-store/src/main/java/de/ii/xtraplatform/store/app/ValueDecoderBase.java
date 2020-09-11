/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.ValueCache;
import de.ii.xtraplatform.store.domain.ValueDecoderMiddleware;
import java.io.IOException;
import java.util.function.Function;

public class ValueDecoderBase<T> implements ValueDecoderMiddleware<T> {

  private final Function<Identifier, T> newInstanceSupplier;
  protected final ValueCache<T> valueCache;

  public ValueDecoderBase(Function<Identifier, T> newInstanceSupplier, ValueCache<T> valueCache) {
    this.newInstanceSupplier = newInstanceSupplier;
    this.valueCache = valueCache;
  }

  @Override
  public T process(Identifier identifier, byte[] payload, ObjectMapper objectMapper, T data)
      throws IOException {
    T data2 = null;

    if (valueCache.isInCache(identifier)) {
      data2 = valueCache.getFromCache(identifier);
    } else {
      data2 = newInstanceSupplier.apply(identifier);
    }

    objectMapper.readerForUpdating(data2).readValue(payload);

    return data2;
  }
}
