/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.redis.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xtraplatform.jobs.domain.JobSet;
import de.ii.xtraplatform.jobs.domain.JobSet.JobSetDetails;
import de.ii.xtraplatform.redis.domain.Redis;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.json.Path2;

@Singleton
public class RedisJobSetOperations {
  private static final String JOB_SET_KEY_PREFIX = "xtraplatform:jobs:set:";
  private final Redis redis;
  private final ObjectMapper mapper;
  private JobDetailsMapper detailsMapper;

  @Inject
  RedisJobSetOperations(Redis redis, ObjectMapper mapper) {
    this.redis = redis;
    this.mapper = mapper;
  }

  public void setDetailsMapper(JobDetailsMapper detailsMapper) {
    this.detailsMapper = detailsMapper;
  }

  public void updateJobSet(JobSet jobSet) {
    try {
      redis.json().jsonSet(JOB_SET_KEY_PREFIX + jobSet.getId(), mapper.writeValueAsString(jobSet));
    } catch (Throwable e) {
      throw new IllegalStateException("Failed to update job set", e);
    }
  }

  public void startJobSet(String jobSetId) {
    redis
        .json()
        .jsonSet(
            JOB_SET_KEY_PREFIX + jobSetId, Path2.of("$.startedAt"), Instant.now().getEpochSecond());
  }

  public void applyJsonPaths(String jobSetId, Map<String, Object> jsonPathUpdates) {
    for (Map.Entry<String, Object> entry : jsonPathUpdates.entrySet()) {
      if (entry.getValue() instanceof Integer) {
        redis
            .json()
            .jsonNumIncrBy(
                JOB_SET_KEY_PREFIX + jobSetId,
                Path2.of(entry.getKey()),
                (Integer) entry.getValue());
        continue;
      }
      redis
          .json()
          .jsonSet(JOB_SET_KEY_PREFIX + jobSetId, Path2.of(entry.getKey()), entry.getValue());
    }
  }

  public Optional<JobSet> getJobSet(String setId) {
    String jobSetJson =
        redis.json().jsonGetAsPlainString(JOB_SET_KEY_PREFIX + setId, Path.ROOT_PATH);

    if (Objects.isNull(jobSetJson)) {
      return Optional.empty();
    }

    try {
      JobSet jobSet = mapper.readValue(jobSetJson, JobSet.class);
      return Optional.ofNullable(jobSet);
    } catch (Throwable e) {
      throw new IllegalStateException("Failed to deserialize job set", e);
    }
  }

  public boolean deleteJobSet(String jobSetId) {
    long count = redis.json().jsonDel(JOB_SET_KEY_PREFIX + jobSetId);
    return count > 0;
  }

  public void initJobSet(JobSet jobSet, int progressDelta, Map<String, Object> detailParameters) {
    Map<String, Object> jsonPathUpdates = new LinkedHashMap<>();
    jsonPathUpdates.put("$.total", progressDelta);

    if (detailsMapper != null) {
      JobSetDetails details = detailsMapper.getJobSetDetails(JobSetDetails.class, jobSet);
      jsonPathUpdates.putAll(details.initJson(detailParameters));
    }

    applyJsonPaths(jobSet.getId(), jsonPathUpdates);
  }

  public void updateJobSetWithProgress(
      JobSet jobSet, int progressDelta, Map<String, Object> detailParameters) {
    Map<String, Object> jsonPathUpdates = new LinkedHashMap<>();
    jsonPathUpdates.put("$.current", progressDelta);
    jsonPathUpdates.put("$.updatedAt", Instant.now().getEpochSecond());

    if (detailsMapper != null) {
      JobSetDetails details = detailsMapper.getJobSetDetails(JobSetDetails.class, jobSet);
      jsonPathUpdates.putAll(details.updateJson(detailParameters));
    }

    applyJsonPaths(jobSet.getId(), jsonPathUpdates);
  }
}
