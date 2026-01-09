/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cache.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.cache.domain.CacheDriver;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
@SuppressWarnings("PMD.TooManyMethods")
public class CacheDriverMem implements CacheDriver {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheDriverMem.class);

  private static final String HASH = "hash";
  private static final String CONTENT = "content";
  private static final String TTL = "ttl";

  private final Map<String, Map<String, Object>> cache;

  @Inject
  public CacheDriverMem() {
    this.cache = new ConcurrentHashMap<>();
  }

  @Override
  public String getType() {
    return "MEM";
  }

  @Override
  public boolean init() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Started {} cache", getType());
    }
    return true;
  }

  @Override
  public boolean has(String key) {
    checkExpired(key);

    return cache.containsKey(key);
  }

  @Override
  public boolean has(String key, String validator) {
    return has(key) && Objects.equals(cache.get(key).get(HASH), validator);
  }

  @Override
  public <T> Optional<T> get(String key, Class<T> clazz) {
    return has(key)
        ? Optional.ofNullable(cache.get(key).get(CONTENT)).map(clazz::cast)
        : Optional.empty();
  }

  @Override
  public <T> Optional<T> get(String key, String validator, Class<T> clazz) {
    return has(key, validator)
        ? Optional.ofNullable(cache.get(key).get(CONTENT)).map(clazz::cast)
        : Optional.empty();
  }

  @Override
  public void put(String key, Object value) {
    set(key, entry(value));
  }

  @Override
  public void put(String key, Object value, int ttl) {
    set(key, entry(value, ttl));
  }

  @Override
  public void put(String key, String validator, Object value) {
    set(key, entry(validator, value));
  }

  @Override
  public void put(String key, String validator, Object value, int ttl) {
    set(key, entry(validator, value, ttl));
  }

  @Override
  public void del(String key) {
    delete(key);
  }

  private void set(String key, Map<String, Object> entry) {
    synchronized (cache) {
      cache.put(key, entry);
    }
  }

  private void delete(String key) {
    synchronized (cache) {
      cache.remove(key);
    }
  }

  private void checkExpired(String key) {
    boolean expired =
        cache.containsKey(key)
            && cache.get(key).containsKey(TTL)
            && ((Long) cache.get(key).get(TTL)) < Instant.now().toEpochMilli();

    if (expired) {
      delete(key);
    }
  }

  private static Map<String, Object> entry(Object value) {
    Map<String, Object> entry = new HashMap<>();
    entry.put(CONTENT, value);

    return entry;
  }

  private static Map<String, Object> entry(Object value, int ttl) {
    Map<String, Object> entry = entry(value);

    long expires = Instant.now().toEpochMilli() + (ttl * 1000L);
    entry.put(TTL, expires);

    return entry;
  }

  private static Map<String, Object> entry(String hash, Object value) {
    Map<String, Object> entry = entry(value);

    entry.put(HASH, hash);

    return entry;
  }

  private static Map<String, Object> entry(String hash, Object value, int ttl) {
    Map<String, Object> entry = entry(value, ttl);

    entry.put(HASH, hash);

    return entry;
  }
}
