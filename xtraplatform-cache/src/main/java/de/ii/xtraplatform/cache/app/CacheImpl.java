/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cache.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import dagger.Lazy;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.cache.domain.Cache;
import de.ii.xtraplatform.cache.domain.CacheDriver;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class CacheImpl implements Cache, AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheImpl.class);

  private final Lazy<Set<CacheDriver>> drivers;

  private CacheDriver driver;

  @Inject
  public CacheImpl(Lazy<Set<CacheDriver>> drivers) {
    this.drivers = drivers;
  }

  @Override
  public CompletionStage<Void> onStart(boolean isStartupAsync) {
    String type = "FS";
    this.driver =
        drivers.get().stream()
            .filter(cacheDriver -> Objects.equals(cacheDriver.getType(), type))
            .findFirst()
            .orElseGet(
                () -> {
                  LOGGER.error("Cache driver with type {} not found, falling back to MEM", type);
                  return new CacheDriverMem();
                });
    driver.init();

    return AppLifeCycle.super.onStart(isStartupAsync);
  }

  @Override
  public boolean has(String... key) {
    return driver.has(key(key));
  }

  @Override
  public boolean hasValid(String validator, String... key) {
    return driver.has(key(key), validator);
  }

  @Override
  public <T> Optional<T> get(Class<T> clazz, String... key) {
    return driver.get(key(key), clazz);
  }

  @Override
  public <T> Optional<T> get(String validator, Class<T> clazz, String... key) {
    return driver.get(key(key), validator, clazz);
  }

  @Override
  public void put(Object value, String... key) {
    driver.put(key(key), value);
  }

  @Override
  public void put(Object value, int ttl, String... key) {
    driver.put(key(key), value, ttl);
  }

  @Override
  public void put(String validator, Object value, String... key) {
    driver.put(key(key), validator, value);
  }

  @Override
  public void put(String validator, Object value, int ttl, String... key) {
    driver.put(key(key), validator, value, ttl);
  }

  @Override
  public void del(String... key) {
    driver.del(key(key));
  }

  private static String key(String... key) {
    return String.join(":", key);
  }
}
