/*
 * Copyright 2025 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public interface MapStreams {
  static <T, U> Collector<Map.Entry<T, U>, ?, Map<T, U>> toMap() {
    return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
  }

  static <T, U> Collector<Map.Entry<T, U>, ?, Map<T, U>> toUnmodifiableMap() {
    return Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue);
  }

  static <T, U> Collector<Map.Entry<T, U>, ?, ImmutableMap<T, U>> toImmutableMap() {
    return ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue);
  }
}
