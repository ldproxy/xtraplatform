/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cache.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import java.util.Optional;

@AutoMultiBind
public interface CacheDriver {
  String getType();

  boolean init();

  boolean has(String key);

  boolean has(String key, String validator);

  <T> Optional<T> get(String key, Class<T> clazz);

  <T> Optional<T> get(String key, String validator, Class<T> clazz);

  void put(String key, Object value);

  void put(String key, Object value, int ttl);

  void put(String key, String validator, Object value);

  void put(String key, String validator, Object value, int ttl);

  void del(String key);
}
