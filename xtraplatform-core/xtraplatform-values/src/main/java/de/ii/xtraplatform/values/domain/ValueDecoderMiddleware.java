/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public interface ValueDecoderMiddleware<T> {

  T process(
      Identifier identifier, byte[] payload, ObjectMapper objectMapper, T data, boolean ignoreCache)
      throws IOException;

  default T recover(Identifier identifier, byte[] payload, ObjectMapper objectMapper)
      throws IOException {
    throw new IllegalStateException();
  }

  default boolean canRecover() {
    return false;
  }
}
