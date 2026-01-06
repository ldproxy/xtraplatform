/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.redis.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.JobsConfiguration.QUEUE;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.jobs.domain.AbstractJobQueueBackend;
import de.ii.xtraplatform.jobs.domain.BaseJob;
import de.ii.xtraplatform.jobs.domain.Job;
import de.ii.xtraplatform.jobs.domain.JobQueueBackend;
import de.ii.xtraplatform.jobs.domain.JobSet;
import de.ii.xtraplatform.redis.domain.Redis;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind(interfaces = JobQueueBackend.class)
public class JobQueueBackendRedis extends AbstractJobQueueBackend<String>
    implements JobQueueBackend {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobQueueBackendRedis.class);

  private final boolean enabled;
  private final RedisJobOperations jobOps;
  private final RedisJobSetOperations jobSetOps;
  private final RedisQueueOperations queueOps;
  private final RedisMetadataOperations metadataOps;
  private final RedisJobLifecycleManager lifecycleManager;
  private final JobDetailsMapper detailsMapper;

  @Inject
  JobQueueBackendRedis(
      AppContext appContext,
      VolatileRegistry volatileRegistry,
      Redis redis,
      RedisJobOperations jobOps,
      RedisJobSetOperations jobSetOps,
      RedisQueueOperations queueOps,
      RedisMetadataOperations metadataOps,
      RedisJobLifecycleManager lifecycleManager,
      JobDetailsMapper detailsMapper) {
    super(volatileRegistry);

    this.enabled = appContext.getConfiguration().getJobs().getQueue() == QUEUE.REDIS;
    this.jobOps = jobOps;
    this.jobSetOps = jobSetOps;
    this.queueOps = queueOps;
    this.metadataOps = metadataOps;
    this.lifecycleManager = lifecycleManager;
    this.detailsMapper = detailsMapper;

    // Initialize cross-dependencies
    jobSetOps.setDetailsMapper(detailsMapper);

    onVolatileStart();
    addSubcomponent(redis);
    onVolatileStarted();
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setJobTypes(Function<String, Optional<? extends Class<?>>> jobTypesMapper) {
    detailsMapper.setJobTypes(jobTypesMapper);
  }

  @Override
  protected String createQueue(String type, int priority) {
    return metadataOps.createQueue(type, priority);
  }

  @Override
  protected Set<String> getTypes() {
    return metadataOps.getTypes();
  }

  @Override
  protected Set<Integer> getPriorities(String type) {
    return metadataOps.getPriorities(type);
  }

  @Override
  protected void updateJob(Job job) {
    jobOps.updateJob(job);
  }

  @Override
  public void updateJob(Job job, int progressDelta) {
    jobOps.updateJobProgress(job.getId(), progressDelta);
  }

  @Override
  protected void updateJobSet(JobSet jobSet) {
    jobSetOps.updateJobSet(jobSet);
  }

  @Override
  public void startJobSet(JobSet jobSet) {
    jobSetOps.startJobSet(jobSet.getId());
  }

  @Override
  public void initJobSet(JobSet jobSet, int progressDelta, Map<String, Object> detailParameters) {
    jobSetOps.initJobSet(jobSet, progressDelta, detailParameters);
  }

  @Override
  public void updateJobSet(JobSet jobSet, int progressDelta, Map<String, Object> detailParameters) {
    jobSetOps.updateJobSetWithProgress(jobSet, progressDelta, detailParameters);
  }

  @Override
  protected Optional<JobSet> getJobSet(String setId) {
    return jobSetOps.getJobSet(setId);
  }

  @Override
  protected void queueJob(Job job, boolean untake) {
    String queue = getQueue(job.getType(), job.getPriority());
    updateJob(job);
    queueOps.queueJob(queue, job.getId(), untake);
  }

  @Override
  protected Job resetJob(Job job, Optional<JobSet> jobSet) {
    return lifecycleManager.resetJob(job, jobSet, jobSetOps);
  }

  @Override
  protected Job startJob(Job job, String executor) {
    return lifecycleManager.startJob(job, executor);
  }

  @Override
  protected Job failJob(Job job, String error) {
    return lifecycleManager.failJob(job, error);
  }

  @Override
  protected Job doneJob(Job job) {
    return lifecycleManager.doneJob(job);
  }

  @Override
  protected Optional<Job> takeJob(String queue) {
    Optional<String> jobId = queueOps.takeJob(queue);
    return jobId.flatMap(this::getJob);
  }

  @Override
  protected Optional<Job> untakeJob(String jobId) {
    if (queueOps.untakeJob(jobId)) {
      return getJob(jobId);
    }
    return Optional.empty();
  }

  @Override
  protected List<? extends BaseJob> onJobFinished(Job job, JobSet jobSet) {
    return lifecycleManager.onJobFinished(job, jobSet);
  }

  @Override
  protected List<Job> getJobsInQueue(String queue) {
    List<String> jobIds = queueOps.getJobsInQueue(queue);
    return jobIds.stream()
        .map(this::getJob)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  @Override
  protected void notifyObservers(String type) {
    queueOps.notifyObservers(type);
  }

  @Override
  public void onPush(Consumer<String> callback) {
    queueOps.onPush(callback);
  }

  @Override
  public boolean doneSet(String jobSetId) {
    return jobSetOps.deleteJobSet(jobSetId);
  }

  @Override
  public boolean error(String jobId, String error, boolean retry) {
    // TODO: retry logic
    return false;
  }

  @Override
  public Collection<JobSet> getSets() {
    return metadataOps.getSets();
  }

  @Override
  protected List<String> getTakenIds() {
    return queueOps.getTakenIds();
  }

  @Override
  protected List<String> getFailedIds() {
    return queueOps.getFailedIds();
  }

  @Override
  protected Optional<Job> getJob(String jobId) {
    return jobOps.getJob(jobId);
  }

  @Override
  public <T> T getJobDetails(Class<T> detailsType, Job job) {
    return detailsMapper.getJobDetails(detailsType, job);
  }

  @Override
  public <T> T getJobSetDetails(Class<T> detailsType, JobSet jobSet) {
    return detailsMapper.getJobSetDetails(detailsType, jobSet);
  }
}
