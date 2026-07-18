/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.redis.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.cache.domain.CacheDriver;
import de.ii.xtraplatform.redis.domain.Redis;
import de.ii.xtraplatform.values.api.ValueEncodingJackson;
import de.ii.xtraplatform.values.domain.ValueEncoding;
import de.ii.xtraplatform.values.domain.ValueEncoding.FORMAT;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.commands.JedisBinaryCommands;

/**
 * Redis-backed CacheDriver, sharing the same Redis connection as JobQueueBackendRedis (s. Redis,
 * injected). Mirrors CacheDriverFs's shape: one Hash per key, the "validator" argument is the Hash
 * field name (CONTENT for the plain 2-arg has/get/put), so a single key can hold several
 * independently addressable entries under different validators, same as CacheDriverFs's
 * one-file-per-validator directory layout.
 *
 * <p>Two deliberate deviations from CacheDriverFs, neither changes observable behavior:
 *
 * <ul>
 *   <li>No key hashing - CacheDriverFs hashes the key into a filesystem-safe directory name; Redis
 *       keys have no such restriction, so the key is used as-is (only prefixed), which also keeps
 *       it readable in redis-cli.
 *   <li>No manual TTL bookkeeping/expiry check - CacheDriverFs stores an expiry timestamp itself
 *       and checks it on every access. expire/persist on the whole Hash key does the same job
 *       natively; once it elapses, Redis removes the key on its own, so has/get simply observe it
 *       as absent. TTL applies to the whole key (all validators together), exactly like
 *       CacheDriverFs's single ttl file per key directory, not per validator.
 * </ul>
 *
 * <p>Values are serialized identically to CacheDriverFs (String passthrough as UTF-8, everything
 * else via Jackson SMILE) and stored as raw bytes via Redis.binary() (JedisBinaryCommands), which
 * is binary-safe - no Base64 detour needed.
 *
 * <p>Redis.binary() can legitimately return null - RedisImpl only actually connects lazily from its
 * periodic Volatile2 health check (s. RedisImpl's check()/connect()), not synchronously at startup,
 * so there's a window (and, if the connection is ever lost, an ongoing possibility) where no client
 * is available yet. Every method here treats that the same as a cache miss/no-op rather than
 * throwing, instead of assuming binary() is always ready like CacheDriverFs's filesystem access
 * always is.
 */
@Singleton
@AutoBind
public class CacheDriverRedis implements CacheDriver {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheDriverRedis.class);

  private static final String KEY_PREFIX = "xtraplatform:cache:";
  private static final String CONTENT = "content";

  private final Redis redis;
  private final ValueEncoding<Object> valueEncoding;

  @Inject
  public CacheDriverRedis(Redis redis, Jackson jackson) {
    this.redis = redis;
    this.valueEncoding = new ValueEncodingJackson<>(jackson, null, false);
  }

  @Override
  public String getType() {
    return "REDIS";
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
    return has(key, CONTENT);
  }

  @Override
  public boolean has(String key, String validator) {
    JedisBinaryCommands cmd = cmd();
    if (Objects.isNull(cmd)) {
      return false;
    }

    boolean exists = cmd.hexists(redisKey(key), validatorField(validator));
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Cache has({}, {}) -> {}", key, validator, exists);
    }
    return exists;
  }

  @Override
  public <T> Optional<T> get(String key, Class<T> clazz) {
    return get(key, CONTENT, clazz);
  }

  @Override
  public <T> Optional<T> get(String key, String validator, Class<T> clazz) {
    JedisBinaryCommands cmd = cmd();
    if (Objects.isNull(cmd)) {
      return Optional.empty();
    }

    byte[] value = cmd.hget(redisKey(key), validatorField(validator));
    if (Objects.isNull(value)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Cache get({}, {}) -> miss", key, validator);
      }
      return Optional.empty();
    }

    try {
      T deserialized = deserialize(value, clazz);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Cache get({}, {}) -> hit ({})", key, validator, deserialized);
      }
      return Optional.ofNullable(deserialized);
    } catch (IOException e) {
      LOGGER.error("CACHE DESER", e);
      return Optional.empty();
    }
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
    JedisBinaryCommands cmd = cmd();
    if (Objects.isNull(cmd)) {
      return;
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Cache del({})", key);
    }
    cmd.del(redisKey(key));
  }

  private void write(String key, String validator, Object value, int ttl) {
    JedisBinaryCommands cmd = cmd();
    if (Objects.isNull(cmd)) {
      return;
    }

    try {
      byte[] serialized = serialize(value);
      byte[] redisKey = redisKey(key);

      cmd.hset(redisKey, validatorField(validator), serialized);

      if (ttl > 0) {
        cmd.expire(redisKey, ttl);
      } else {
        cmd.persist(redisKey);
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Cache put({}, {}, ttl={})", key, validator, ttl);
      }
    } catch (IOException e) {
      // ignore, same as CacheDriverFs
    }
  }

  private JedisBinaryCommands cmd() {
    JedisBinaryCommands cmd = redis.binary();
    if (Objects.isNull(cmd) && LOGGER.isDebugEnabled()) {
      LOGGER.debug("Cache unavailable, redis is not connected yet");
    }
    return cmd;
  }

  private byte[] redisKey(String key) {
    return (KEY_PREFIX + key).getBytes(StandardCharsets.UTF_8);
  }

  private byte[] validatorField(String validator) {
    return validator.getBytes(StandardCharsets.UTF_8);
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
}
