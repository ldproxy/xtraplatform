/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.redis.app;

import de.ii.xtraplatform.jobs.domain.BaseJob;
import de.ii.xtraplatform.jobs.domain.Job;
import de.ii.xtraplatform.jobs.domain.JobSet;
import de.ii.xtraplatform.jobs.domain.JobSet.JobSetDetails;
import de.ii.xtraplatform.redis.domain.Redis;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RedisJobLifecycleManager {
  private final Redis redis;
  private final RedisJobOperations jobOps;
  private final JobDetailsMapper detailsMapper;

  @Inject
  RedisJobLifecycleManager(Redis redis, RedisJobOperations jobOps, JobDetailsMapper detailsMapper) {
    this.redis = redis;
    this.jobOps = jobOps;
    this.detailsMapper = detailsMapper;
  }

  public Job resetJob(Job job, Optional<JobSet> jobSet, RedisJobSetOperations jobSetOps) {
    if (jobSet.isPresent()) {
      jobSet.get().update(-(job.getCurrent().get()));
      JobSetDetails details = detailsMapper.getJobSetDetails(JobSetDetails.class, jobSet.get());
      details.reset(job);
      jobSetOps.updateJobSet(jobSet.get().with(details));
    }
    return job.reset();
  }

  public Job startJob(Job job, String executor) {
    Job startedJob = job.started(executor);
    jobOps.updateJob(startedJob);
    return startedJob;
  }

  public Job failJob(Job job, String error) {
    Job failedJob = job.failed(error);
    jobOps.updateJob(failedJob);
    redis.cmd().rpush("xtraplatform:jobs:failed", job.getId());
    return failedJob;
  }

  public Job doneJob(Job job) {
    Job doneJob = job.done();
    jobOps.deleteJob(doneJob.getId());
    return doneJob;
  }

  public List<? extends BaseJob> onJobFinished(Job job, JobSet jobSet) {
    List<? extends BaseJob> followUps = jobSet.done(job);
    jobOps.deleteJob(job.getId());
    return followUps;
  }
}
