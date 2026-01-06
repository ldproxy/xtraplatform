/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.redis.app;

import de.ii.xtraplatform.jobs.domain.JobSet;
import de.ii.xtraplatform.redis.domain.Redis;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RedisMetadataOperations {
  private final Redis redis;
  private final RedisJobSetOperations jobSetOps;

  @Inject
  RedisMetadataOperations(Redis redis, RedisJobSetOperations jobSetOps) {
    this.redis = redis;
    this.jobSetOps = jobSetOps;
  }

  public String createQueue(String type, int priority) {
    redis.cmd().zadd("xtraplatform:jobs:priorities:" + type, priority, String.valueOf(priority));
    return "xtraplatform:jobs:queue:" + type + ":" + priority;
  }

  public Set<String> getTypes() {
    return redis.cmd().keys("xtraplatform:jobs:priorities:*").stream()
        .map(key -> key.substring("xtraplatform:jobs:priorities:".length()))
        .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
  }

  public Set<Integer> getPriorities(String type) {
    List<String> priorities = redis.cmd().zrevrange("xtraplatform:jobs:priorities:" + type, 0, -1);
    return new LinkedHashSet<>(priorities.stream().map(Integer::parseInt).toList());
  }

  public Collection<JobSet> getSets() {
    Set<String> jobSetIds = redis.cmd().keys("xtraplatform:jobs:set:*");
    return jobSetIds.stream()
        .map(id -> id.substring("xtraplatform:jobs:set:".length()))
        .map(jobSetOps::getJobSet)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }
}
