/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public interface ValueEncoding<T> {

  ObjectMapper getMapper(FORMAT format);

  enum FORMAT {
    NONE,
    JSON,
    YML,
    YAML,
    UNKNOWN /*, ION*/;

    public static FORMAT fromString(String format) {
      if (Objects.isNull(format)) {
        return NONE;
      }

      for (FORMAT f : values()) {
        if (Objects.equals(f.name(), format.toUpperCase())) {
          return f;
        }
      }

      return UNKNOWN;
    }
  }

  FORMAT getDefaultFormat();

  default boolean isSupported(String format) {
    return FORMAT.fromString(format) != FORMAT.UNKNOWN;
  }

  byte[] serialize(T data);

  byte[] serialize(Map<String, Object> data);

  T deserialize(Identifier identifier, byte[] payload, FORMAT format, boolean ignoreCache)
      throws IOException;

  byte[] nestPayload(
      byte[] payload, String format, List<String> nestingPath, Optional<KeyPathAlias> keyPathAlias)
      throws IOException;
}
