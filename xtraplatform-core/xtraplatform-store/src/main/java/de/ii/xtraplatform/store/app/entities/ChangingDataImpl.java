/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app.entities;

import de.ii.xtraplatform.store.domain.entities.ChangingData;
import de.ii.xtraplatform.store.domain.entities.ChangingValue;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ChangingDataImpl implements ChangingData {

  public static final String AGGREGATED = "__AGGREGATED__";

  private final Map<Class<? extends ChangingValue<?>>, Map<String, ChangingValue<?>>> values;

  public ChangingDataImpl() {
    this.values = new ConcurrentHashMap<>();
  }

  @Override
  public <T, U extends ChangingValue<T>> Optional<U> get(Class<U> type, String id) {
    return Optional.ofNullable(values.get(type))
        .flatMap(m -> Optional.ofNullable(m.get(id)).map(type::cast));
  }

  @Override
  public <T, U extends ChangingValue<T>> Optional<U> get(Class<U> type) {
    return get(type, AGGREGATED);
  }

  @Override
  public <S, T extends ChangingValue<S>> void put(Class<T> type, String id, T value) {
    if (!values.containsKey(type)) {
      values.put(type, new ConcurrentHashMap<>());
    }
    values.get(type).put(id, value);

    updateAggregate(type, value);
  }

  @Override
  public <S, T extends ChangingValue<S>> boolean update(Class<T> type, String id, T delta) {
    if (!values.containsKey(type)) {
      values.put(type, new ConcurrentHashMap<>());
    }
    if (!values.get(type).containsKey(id)) {
      put(type, id, delta);
      return true;
    }

    T current = type.cast(values.get(type).get(id));
    Optional<T> updated = current.updateWith(delta).map(type::cast);

    if (updated.isPresent()) {
      values.get(type).put(id, updated.get());
      updateAggregate(type);
      // TODO: does not work for count
      // updateAggregate(type, updated.get());

      return true;
    }

    return false;
  }

  @Override
  public <T, U extends ChangingValue<T>> boolean remove(Class<U> type, String id) {
    if (values.containsKey(type)) {
      U removed = type.cast(values.get(type).remove(id));
      if (Objects.nonNull(removed)) {
        updateAggregate(type);
        return true;
      }
    }
    return false;
  }

  private <S, T extends ChangingValue<S>> void updateAggregate(Class<T> type, T next) {
    if (values.containsKey(type)) {
      if (values.get(type).containsKey(AGGREGATED)) {
        T current = type.cast(values.get(type).get(AGGREGATED));
        ChangingValue<S> updated = current;

        if (Objects.nonNull(next)) {
          updated = updated.updateWith(next).orElse(updated);
        }

        values.get(type).put(AGGREGATED, updated);
      } else if (Objects.nonNull(next)) {
        values.get(type).put(AGGREGATED, next);
      }
    }
  }

  private <S, T extends ChangingValue<S>> void updateAggregate(Class<T> type) {
    if (values.containsKey(type)) {
      Optional<T> aggregated =
          values.get(type).entrySet().stream()
              .filter(entry -> !Objects.equals(entry.getKey(), AGGREGATED))
              .map(entry -> type.cast(entry.getValue()))
              .reduce((first, second) -> first.updateWith(second).map(type::cast).orElse(first));

      if (aggregated.isPresent()) {
        values.get(type).put(AGGREGATED, aggregated.get());
      }
    }
  }
}
