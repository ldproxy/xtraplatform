/*
 * Copyright 2025 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.redis.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.resiliency.AbstractVolatilePolling;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.redis.domain.Redis;
import de.ii.xtraplatform.redis.domain.RedisPubSub;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.JedisCommands;
import redis.clients.jedis.json.commands.RedisJsonCommands;

@Singleton
@AutoBind
public class RedisImpl extends AbstractVolatilePolling implements Redis, AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedisImpl.class);

  private final List<String> nodes;
  private final boolean asyncStartup;
  private UnifiedJedis jedis;
  private Throwable connectionError;

  @Inject
  RedisImpl(AppContext appContext, VolatileRegistry volatileRegistry) {
    super(volatileRegistry, "app/redis");
    this.nodes = appContext.getConfiguration().getRedis().getNodes();
    this.asyncStartup = appContext.getConfiguration().getModules().isStartupAsync();
  }

  @Override
  public int getPriority() {
    return 40;
  }

  @Override
  public CompletionStage<Void> onStart(boolean isStartupAsync) {
    onVolatileStart();

    return AppLifeCycle.super.onStart(isStartupAsync);
  }

  @Override
  public void onStop() {
    if (jedis != null) {
      jedis.close();
    }

    onVolatileStop();

    AppLifeCycle.super.onStop();
  }

  @Override
  protected void onVolatileStart() {
    super.onVolatileStart();

    synchronized (this) {
      if (asyncStartup) {
        if (getState() == State.UNAVAILABLE) {
          LOGGER.warn("Could not establish connection to redis");
        }

        onStateChange(
            (from, to) -> {
              if (to == State.AVAILABLE) {
                LOGGER.info("Re-established connection to redis");
              } else if (to == State.UNAVAILABLE) {
                LOGGER.warn("Lost connection to redis");
              }
            },
            false);
      }
    }
  }

  @Override
  public JedisCommands cmd() {
    return jedis;
  }

  @Override
  public RedisJsonCommands json() {
    return jedis;
  }

  @Override
  public RedisPubSub pubsub() {
    return new RedisPubSub() {
      @Override
      public void publish(String channel, String message) {
        jedis.publish(channel, message);
      }

      @Override
      public void subscribe(String channel, Consumer<String> subscriber) {
        // LOGGER.debug("REDIS PUBSUB SUBSCRIBE {}", channel);
        jedis.subscribe(
            new JedisPubSub() {
              @Override
              public void onMessage(String ch, String message) {
                // LOGGER.debug("REDIS PUBSUB MESSAGE {} {}", ch, message);
                if (Objects.equals(ch, channel)) {
                  subscriber.accept(message);
                }
              }
            },
            channel);
      }
    };
  }

  @Override
  public int getIntervalMs() {
    return 1000;
  }

  @Override
  public Tuple<State, String> check() {
    if (nodes.isEmpty() /*|| Objects.nonNull(jedis)*/) {
      return Tuple.of(State.AVAILABLE, null);
    }

    connect();

    if (Objects.isNull(jedis)) {
      // TODO: retry
      if (Objects.nonNull(connectionError)) {
        return Tuple.of(State.UNAVAILABLE, connectionError.getMessage());
      }
      return Tuple.of(State.UNAVAILABLE, null);
    }

    try {
      String response = jedis.ping();
      if ("pong".equalsIgnoreCase(response)) {
        return Tuple.of(State.AVAILABLE, null);
      }
    } catch (Throwable e) {
      return Tuple.of(State.UNAVAILABLE, e.getMessage());
    }

    return Tuple.of(State.UNAVAILABLE, null);
  }

  private void connect() {
    if (Objects.nonNull(jedis)) {
      return;
    }

    try {
      if (nodes.size() == 1) {
        this.jedis = new JedisPooled(getHostAndPort(nodes.get(0)));

        // LOGGER.info("REDIS SINGLE NODE {}", getHostAndPort(nodes.get(0)));
        // LOGGER.info("REDIS PING {}", this.jedis.ping());
      } else if (nodes.size() > 1) {
        Set<HostAndPort> jedisClusterNodes =
            nodes.stream().map(RedisImpl::getHostAndPort).collect(Collectors.toSet());
        this.jedis = new JedisCluster(jedisClusterNodes);
        // LOGGER.info("REDIS CLUSTER NODES {}", jedisClusterNodes);
        // LOGGER.info("REDIS PING {}", this.jedis.ping());
      }
    } catch (Throwable e) {
      this.connectionError = e;
    }
  }

  private static HostAndPort getHostAndPort(String node) {
    String[] parts = node.split(":");
    String host = parts[0];
    int port = 6379;
    if (parts.length > 1) {
      port = Integer.parseInt(parts[1]);
    }
    return new HostAndPort(host, port);
  }
}
