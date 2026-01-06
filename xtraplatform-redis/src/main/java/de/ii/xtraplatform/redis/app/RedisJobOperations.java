/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.redis.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xtraplatform.jobs.domain.Job;
import de.ii.xtraplatform.redis.domain.Redis;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.json.Path2;

@Singleton
public class RedisJobOperations {
  private final Redis redis;
  private final ObjectMapper mapper;
  private static final String JOB_KEY_PREFIX = "xtraplatform:jobs:job:";

  @Inject
  RedisJobOperations(Redis redis, ObjectMapper mapper) {
    this.redis = redis;
    this.mapper = mapper;
  }

  public void updateJob(Job job) {
    try {
      redis.json().jsonSet(JOB_KEY_PREFIX + job.getId(), mapper.writeValueAsString(job));
    } catch (Throwable e) {
      throw new IllegalStateException("Failed to update job", e);
    }
  }

  public void updateJobProgress(String jobId, int progressDelta) {
    redis.json().jsonNumIncrBy(JOB_KEY_PREFIX + jobId, Path2.of("$.current"), progressDelta);
    redis
        .json()
        .jsonSet(JOB_KEY_PREFIX + jobId, Path2.of("$.updatedAt"), Instant.now().getEpochSecond());
  }

  public Optional<Job> getJob(String jobId) {
    String jobJson = redis.json().jsonGetAsPlainString(JOB_KEY_PREFIX + jobId, Path.ROOT_PATH);

    if (Objects.isNull(jobJson)) {
      return Optional.empty();
    }

    try {
      Job job = mapper.readValue(jobJson, Job.class);
      return Optional.ofNullable(job);
    } catch (Throwable e) {
      throw new IllegalStateException("Failed to deserialize job", e);
    }
  }

  public void deleteJob(String jobId) {
    redis.json().jsonDel(JOB_KEY_PREFIX + jobId);
  }
}
