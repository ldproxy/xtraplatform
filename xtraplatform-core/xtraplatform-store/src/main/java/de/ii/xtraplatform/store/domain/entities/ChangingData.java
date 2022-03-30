/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain.entities;

import java.util.Optional;

public interface ChangingData {

  /**
   * get a changing value
   *
   * @param type the type
   * @param id the id
   * @return the value, if it exists
   */
  <T, U extends ChangingValue<T>> Optional<U> get(Class<U> type, String id);

  /**
   * get the aggregated changing value for the given type
   *
   * @param type the type
   * @return the value, if it exists
   */
  <T, U extends ChangingValue<T>> Optional<U> get(Class<U> type);

  /**
   * set a changing value
   *
   * @param type the type
   * @param id the id
   * @param value the new metadata value
   */
  <T, U extends ChangingValue<T>> void put(Class<U> type, String id, U value);

  /**
   * update a changing value; merge the new value with the existing value
   *
   * @param type the type
   * @param id the id
   * @param delta the new value
   * @return {@code true}, if there was a value before
   */
  <T, U extends ChangingValue<T>> boolean update(Class<U> type, String id, U delta);

  /**
   * remove a changing value
   *
   * @param type the type
   * @param id the id
   * @return {@code true}, if there was a value to remove
   */
  <T, U extends ChangingValue<T>> boolean remove(Class<U> type, String id);
}
