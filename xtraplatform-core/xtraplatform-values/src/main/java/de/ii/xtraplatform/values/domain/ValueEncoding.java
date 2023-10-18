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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static List<String> extensions(String... additional) {
      return Stream.concat(
              Arrays.stream(values())
                  .filter(format -> format != NONE && format != UNKNOWN)
                  .map(Enum::name),
              Arrays.stream(additional))
          .map(format -> "." + format.toLowerCase())
          .collect(Collectors.toList());
    }

    public String apply(String path) {
      if (this == NONE || this == UNKNOWN) {
        return path;
      }
      return path + "." + this.name().toLowerCase();
    }
  }

  FORMAT getDefaultFormat();

  default boolean isSupported(String format) {
    return FORMAT.fromString(format) != FORMAT.UNKNOWN;
  }

  byte[] serialize(T data);

  byte[] serialize(T data, FORMAT format);

  byte[] serialize(Map<String, Object> data);

  T deserialize(Identifier identifier, byte[] payload, FORMAT format, boolean ignoreCache)
      throws IOException;
}
