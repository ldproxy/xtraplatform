/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.app;

import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.jobs.domain.BaseJob;
import de.ii.xtraplatform.jobs.domain.Job;
import de.ii.xtraplatform.jobs.domain.JobQueue;
import de.ii.xtraplatform.jobs.domain.JobSet;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// @Singleton
// @AutoBind
public class JobQueueRedis implements JobQueue {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobQueueRedis.class);

  // @Inject
  JobQueueRedis(AppContext appContext) {
    // TODO: if enabled, connect with jedis

    // TODO: one queue per type, queue has ids, one hash for each id with details

    // TODO: housekeeping might check taken list using RPOPLPUSH with same source and destination
    // this way it can check for timeouts, then use a transaction with LREM, LPUSH and HMSET to
    // retry
  }

  @Override
  public synchronized void push(BaseJob job, boolean untake) {
    if (job instanceof Job) {
      // TODO: LPUSH xtraplatform:jobs:queue and HMSET xtraplatform:jobs:job:<id>
    } else if (job instanceof JobSet) {
      // TODO: HMSET xtraplatform:jobs:set:<id>
    } else {
      throw new IllegalArgumentException("Unknown job type: " + job.getClass());
    }
  }

  @Override
  public synchronized Optional<Job> take(String type, String executor) {
    // TODO: RPOPLPUSH xtraplatform:jobs:queue xtraplatform:jobs:progress and HMSET
    // xtraplatform:jobs:job:<id>

    return Optional.empty();
  }

  @Override
  public synchronized boolean done(String jobId) {
    // TODO: LREM xtraplatform:jobs:progress 0 <id>

    // TODO: if > 0 then HMGET xtraplatform:jobs:job:<id>

    // TODO: if partOf then HINCRBY xtraplatform:jobs:set:<partOf> current 1 (if done, mark for
    // removal)

    // TODO: if followUps then LPUSH xtraplatform:jobs:queue <followUps>

    // TODO: HDEL xtraplatform:jobs:job:<id>

    return false;
  }

  @Override
  public synchronized boolean error(String jobId, String error, boolean retry) {
    // TODO: retry logic
    return false;
  }

  @Override
  public Collection<JobSet> getSets() {
    return List.of();
  }

  @Override
  public Map<String, Deque<Job>> getOpen() {
    return Map.of();
  }

  @Override
  public Collection<Job> getTaken() {
    return List.of();
  }

  @Override
  public JobSet getSet(String setId) {
    return null;
  }
}
