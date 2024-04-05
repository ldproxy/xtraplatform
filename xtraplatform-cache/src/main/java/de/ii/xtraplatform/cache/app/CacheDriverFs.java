/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cache.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.hash.Hashing;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.cache.domain.CacheDriver;
import de.ii.xtraplatform.values.api.ValueEncodingJackson;
import de.ii.xtraplatform.values.domain.ValueEncoding;
import de.ii.xtraplatform.values.domain.ValueEncoding.FORMAT;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class CacheDriverFs implements CacheDriver {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheDriverFs.class);

  private static final String HASH = "hash";
  private static final String CONTENT = "content";
  private static final String TTL = "ttl";

  private final Path cache;
  private final ValueEncoding<Object> valueEncoding;

  @Inject
  public CacheDriverFs(AppContext appContext, Jackson jackson) {
    this.cache = appContext.getTmpDir().resolve("cache");
    this.valueEncoding = new ValueEncodingJackson<>(jackson, false);
  }

  @Override
  public String getType() {
    return "FS";
  }

  @Override
  public boolean init() {
    try {
      Files.createDirectories(cache);
    } catch (IOException e) {
      LogContext.errorAsWarn(LOGGER, e, "Could not start {} cache", getType());
      return false;
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Started {} cache", getType());
    }
    return true;
  }

  @Override
  public boolean has(String key) {
    return exists(key, CONTENT);
  }

  @Override
  public boolean has(String key, String validator) {
    return exists(key, validator);
  }

  @Override
  public <T> Optional<T> get(String key, Class<T> clazz) {
    return has(key) ? Optional.ofNullable(read(key, CONTENT, clazz)) : Optional.empty();
  }

  @Override
  public <T> Optional<T> get(String key, String validator, Class<T> clazz) {
    return has(key, validator)
        ? Optional.ofNullable(read(key, validator, clazz))
        : Optional.empty();
  }

  @Override
  public void put(String key, Object value) {
    write(key, CONTENT, value, 0);
  }

  @Override
  public void put(String key, Object value, int ttl) {
    write(key, CONTENT, value, ttl);
  }

  @Override
  public void put(String key, String validator, Object value) {
    write(key, validator, value, 0);
  }

  @Override
  public void put(String key, String validator, Object value, int ttl) {
    write(key, validator, value, ttl);
  }

  @Override
  public void del(String key) {
    delete(key);
  }

  private boolean exists(String key, String validator) {
    checkExpired(key);

    Path content = keyPath(key).resolve(validator);

    return Files.exists(content);
  }

  private <T> T read(String key, String validator, Class<T> clazz) {
    Path content = keyPath(key).resolve(validator);

    try {
      byte[] serialized = Files.readAllBytes(content);

      return deserialize(serialized, clazz);
    } catch (IOException e) {
      LOGGER.error("CACHE DESER", e);
      return null;
    }
  }

  private synchronized void write(String key, String validator, Object value, int ttl) {
    Path entry = keyPath(key);
    try {
      byte[] serialized = serialize(value);

      Files.createDirectories(entry);
      Files.write(entry.resolve(validator), serialized);
      if (ttl > 0) {
        long expires = Instant.now().toEpochMilli() + (ttl * 1000L);
        Files.writeString(entry.resolve(TTL), Long.toString(expires));
      } else {
        Files.deleteIfExists(entry.resolve(TTL));
      }
    } catch (IOException e) {
      // ignore
    }
  }

  private Path keyPath(String key) {
    return cache.resolve(Hashing.murmur3_128().hashString(key, StandardCharsets.UTF_8).toString());
  }

  private byte[] serialize(Object obj) throws IOException {
    if (obj instanceof String) {
      return ((String) obj).getBytes(StandardCharsets.UTF_8);
    }

    return valueEncoding.serialize(obj, FORMAT.SMILE);
  }

  private <T> T deserialize(byte[] value, Class<T> clazz) throws IOException {
    if (String.class.equals(clazz)) {
      return clazz.cast(new String(value, StandardCharsets.UTF_8));
    }

    return valueEncoding.getMapper(FORMAT.SMILE).readValue(value, clazz);
  }

  private synchronized void delete(String key) {
    try (Stream<Path> entries = Files.walk(keyPath(key))) {
      entries.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    } catch (IOException e) {
      // ignore
    }
  }

  private void checkExpired(String key) {
    Path entry = keyPath(key);
    boolean expired = false;

    try {
      expired =
          Files.exists(entry)
              && Files.exists(entry.resolve(TTL))
              && Long.parseLong(Files.readString(entry.resolve(TTL)))
                  < Instant.now().toEpochMilli();
    } catch (IOException e) {
      // ignore
    }

    if (expired) {
      delete(key);
    }
  }
}
