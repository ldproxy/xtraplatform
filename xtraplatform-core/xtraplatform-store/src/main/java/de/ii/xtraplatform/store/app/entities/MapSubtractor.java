/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app.entities;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MapSubtractor {

  public Map<String, Object> subtract(
      Map<String, Object> data, Map<String, Object> defaults, List<String> ignoreKeys) {

    if (Objects.equals(data, defaults)) {
      return ImmutableMap.of();
    }

    ImmutableMap.Builder<String, Object> result = ImmutableMap.builder();

    MapDifference<String, Object> difference = Maps.difference(data, defaults);

    Map<String, Object> newEntries = difference.entriesOnlyOnLeft();
    Map<String, ValueDifference<Object>> differingEntries = difference.entriesDiffering();

    // result.putAll(newEntries);

    for (String key : data.keySet()) {
      if (ignoreKeys.contains(key)) {
        result.put(key, data.get(key));
      }

      if (newEntries.containsKey(key)) {
        result.put(key, newEntries.get(key));
      }

      if (differingEntries.containsKey(key)) {
        ValueDifference<Object> diff = differingEntries.get(key);

        if (diff.leftValue() instanceof Map) {
          result.put(
              key,
              subtract(
                  (Map<String, Object>) diff.leftValue(),
                  (Map<String, Object>) diff.rightValue(),
                  ignoreKeys));

          continue;
        }
        if (diff.leftValue() instanceof Collection) {
          result.put(
              key,
              subtract(
                  (Collection<Object>) diff.leftValue(), (Collection<Object>) diff.rightValue()));

          continue;
        }

        result.put(key, diff.leftValue());
      }
    }

    return result.build();
  }

  private Collection<Object> subtract(Collection<Object> left, Collection<Object> right) {
    ArrayList<Object> diff = Lists.newArrayList(left);

    for (Object item : right) {
      diff.remove(item);
    }

    return diff;
  }

  private Map<String, Object> entriesDiffering(MapDifference<String, Object> difference) {
    return difference.entriesDiffering().entrySet().stream()
        .map(entry -> new SimpleEntry<>(entry.getKey(), entry.getValue().rightValue()))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private boolean containsMaps(Collection<?> collection) {
    return !collection.isEmpty() && collection.iterator().next() instanceof Map;
  }
}
