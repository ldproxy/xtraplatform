/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app.entities;

import de.ii.xtraplatform.store.domain.KeyPathAliasUnwrap;
import de.ii.xtraplatform.store.domain.entities.EntityFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MapAligner {

  public static Map<String, Object> align(
      Map<String, Object> source,
      Map<String, Object> alignTo,
      Predicate<Object> alignIf,
      EntityFactory entityFactory) {
    Map<String, Object> result = new LinkedHashMap<>();

    source.forEach(
        (key, value) -> {
          if (alignTo.containsKey(key) && alignIf.test(alignTo.get(key))) {
            if (Objects.nonNull(alignTo.get(key))) {
              result.put(key, alignTo.get(key));
            }
            return;
          }

          Object newValue =
              value instanceof Map && alignTo.get(key) instanceof Map
                  ? align(
                      (Map<String, Object>) value,
                      (Map<String, Object>) alignTo.get(key),
                      alignIf,
                      entityFactory)
                  : value instanceof List && alignTo.get(key) instanceof List
                      ? align(
                          (List<Object>) value,
                          (List<Object>) alignTo.get(key),
                          alignIf,
                          entityFactory,
                          key)
                      : value instanceof List && alignTo.get(key) instanceof Map
                          ? align(
                              (List<Object>) value, (Map<String, Object>) alignTo.get(key), alignIf)
                          : value;

          result.put(key, newValue);
        });

    return result;
  }

  private static List<Object> align(
      List<Object> source,
      List<Object> alignTo,
      Predicate<Object> alignIf,
      EntityFactory entityFactory,
      String parentKey) {
    if (entityFactory.getKeyPathAliasReverse(parentKey).isEmpty()) {
      return source;
    }

    List<Object> result = new ArrayList<>();
    KeyPathAliasUnwrap aliasUnwrap = entityFactory.getKeyPathAliasReverse(parentKey).get();

    Map<String, Object> aligned =
        align(aliasUnwrap.wrapMap(source), aliasUnwrap.wrapMap(alignTo), alignIf, entityFactory);
    aligned.forEach(
        (s, o) -> {
          ((Map<String, Object>) o).remove("buildingBlock");
        });

    return aligned.entrySet().stream()
        .flatMap(
            entry -> {
              return entityFactory
                  .getKeyPathAlias(entry.getKey())
                  .map(
                      keyPathAlias1 -> {
                        return keyPathAlias1
                            .wrapMap((Map<String, Object>) entry.getValue())
                            .values()
                            .stream()
                            .flatMap(
                                coll -> {
                                  return ((List<Object>) coll).stream();
                                });
                      })
                  .orElse(Stream.empty());
            })
        .collect(Collectors.toList());
  }

  private static List<Object> align(
      List<Object> source, Map<String, Object> alignTo, Predicate<Object> alignIf) {
    List<Object> result = new ArrayList<>();

    source.forEach(
        item -> {
          if (alignTo.containsKey(item) && alignIf.test(alignTo.get(item))) {
            if (Objects.nonNull(alignTo.get(item))) {
              result.add(alignTo.get(item));
            }
            return;
          }

          result.add(item);
        });

    return result;
  }
}
