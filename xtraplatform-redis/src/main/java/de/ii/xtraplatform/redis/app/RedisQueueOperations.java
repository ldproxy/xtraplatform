/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.redis.app;

import de.ii.xtraplatform.redis.domain.Redis;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import redis.clients.jedis.args.ListDirection;

@Singleton
public class RedisQueueOperations {
  private static final String TAKEN_KEY = "xtraplatform:jobs:taken";
  private final Redis redis;

  @Inject
  RedisQueueOperations(Redis redis) {
    this.redis = redis;
  }

  public void notifyObservers(String type) {
    redis.pubsub().publish("xtraplatform:jobs:notifications", type);
  }

  public void onPush(Consumer<String> callback) {
    redis.pubsub().subscribe("xtraplatform:jobs:notifications", callback);
  }

  public void queueJob(String queue, String jobId, boolean untake) {
    if (untake) {
      redis.cmd().lrem(TAKEN_KEY, 1, jobId);
      redis.cmd().rpush(queue, jobId);
    } else {
      redis.cmd().lpush(queue, jobId);
    }
  }

  public Optional<String> takeJob(String queue) {
    String jobId = redis.cmd().lmove(queue, TAKEN_KEY, ListDirection.RIGHT, ListDirection.LEFT);
    return Optional.ofNullable(jobId);
  }

  public boolean untakeJob(String jobId) {
    long count = redis.cmd().lrem(TAKEN_KEY, 1, jobId);
    return count > 0;
  }

  public List<String> getJobsInQueue(String queue) {
    return redis.cmd().lrange(queue, 0, -1);
  }

  public List<String> getTakenIds() {
    return redis.cmd().lrange(TAKEN_KEY, 0, -1);
  }

  public List<String> getFailedIds() {
    return redis.cmd().lrange("xtraplatform:jobs:failed", 0, -1);
  }
}
