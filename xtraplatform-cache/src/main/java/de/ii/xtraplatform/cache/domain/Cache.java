/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cache.domain;

import com.google.common.collect.ObjectArrays;
import java.util.Optional;

public interface Cache {

  boolean has(String... key);

  boolean hasValid(String validator, String... key);

  default Optional<String> get(String... key) {
    return get(String.class, key);
  }

  default Optional<String> getValid(String validator, String... key) {
    return get(validator, String.class, key);
  }

  <T> Optional<T> get(Class<T> clazz, String... key);

  <T> Optional<T> get(String validator, Class<T> clazz, String... key);

  void put(Object value, String... key);

  void put(Object value, int ttl, String... key);

  void put(String validator, Object value, String... key);

  void put(String validator, Object value, int ttl, String... key);

  void del(String... key);

  default Cache withPrefix(String... prefix) {
    Cache delegate = this;

    return new Cache() {
      @Override
      public boolean has(String... key) {
        return delegate.has(ObjectArrays.concat(prefix, key, String.class));
      }

      @Override
      public boolean hasValid(String validator, String... key) {
        return delegate.hasValid(validator, ObjectArrays.concat(prefix, key, String.class));
      }

      @Override
      public <T> Optional<T> get(Class<T> clazz, String... key) {
        return delegate.get(clazz, ObjectArrays.concat(prefix, key, String.class));
      }

      @Override
      public <T> Optional<T> get(String validator, Class<T> clazz, String... key) {
        return delegate.get(validator, clazz, ObjectArrays.concat(prefix, key, String.class));
      }

      @Override
      public void put(Object value, String... key) {
        delegate.put(value, ObjectArrays.concat(prefix, key, String.class));
      }

      @Override
      public void put(Object value, int ttl, String... key) {
        delegate.put(value, ttl, ObjectArrays.concat(prefix, key, String.class));
      }

      @Override
      public void put(String validator, Object value, String... key) {
        delegate.put(validator, value, ObjectArrays.concat(prefix, key, String.class));
      }

      @Override
      public void put(String validator, Object value, int ttl, String... key) {
        delegate.put(validator, value, ttl, ObjectArrays.concat(prefix, key, String.class));
      }

      @Override
      public void del(String... key) {
        delegate.del(ObjectArrays.concat(prefix, key, String.class));
      }
    };
  }
}
