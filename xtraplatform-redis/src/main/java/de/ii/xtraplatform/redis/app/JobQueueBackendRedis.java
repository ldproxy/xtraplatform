/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.redis.app;

import static de.ii.xtraplatform.base.domain.util.JacksonModules.DESERIALIZE_IMMUTABLE_BUILDER_NESTED;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.JobsConfiguration.QUEUE;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.jobs.domain.AbstractJobQueueBackend;
import de.ii.xtraplatform.jobs.domain.BaseJob;
import de.ii.xtraplatform.jobs.domain.Job;
import de.ii.xtraplatform.jobs.domain.Job.JobDetails;
import de.ii.xtraplatform.jobs.domain.JobQueueBackend;
import de.ii.xtraplatform.jobs.domain.JobSet;
import de.ii.xtraplatform.jobs.domain.JobSet.JobSetDetails;
import de.ii.xtraplatform.redis.domain.Redis;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.args.ListDirection;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.json.Path2;

@Singleton
@AutoBind(interfaces = JobQueueBackend.class)
@SuppressWarnings("PMD.TooManyMethods")
public class JobQueueBackendRedis extends AbstractJobQueueBackend<String>
    implements JobQueueBackend {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobQueueBackendRedis.class);
  private static final List<Integer> INITIAL_LEVELS =
      IntStream.range(0, 24).map(i -> -1).boxed().toList();

  // Redis key constants
  private static final String REDIS_KEY_PRIORITIES = "xtraplatform:jobs:priorities:";
  private static final String REDIS_KEY_QUEUE = "xtraplatform:jobs:queue:";
  private static final String REDIS_KEY_JOB = "xtraplatform:jobs:job:";
  private static final String REDIS_KEY_SET = "xtraplatform:jobs:set:";
  private static final String REDIS_KEY_TAKEN = "xtraplatform:jobs:taken";
  private static final String REDIS_KEY_FAILED = "xtraplatform:jobs:failed";
  private static final String REDIS_KEY_NOTIFICATIONS = "xtraplatform:jobs:notifications";

  private final boolean enabled;
  private final Redis redis;
  private final ObjectMapper mapper;
  private Function<String, Optional<? extends Class<?>>> jobTypes;

  @Inject
  JobQueueBackendRedis(
      AppContext appContext, Jackson jackson, VolatileRegistry volatileRegistry, Redis redis) {
    super(volatileRegistry);

    // NOPMD - TODO: housekeeping might check taken list using RPOPLPUSH with same source and
    // destination
    // this way it can check for timeouts, then use a transaction with LREM, LPUSH and HMSET to
    // retry

    this.enabled = appContext.getConfiguration().getJobs().getQueue() == QUEUE.REDIS;
    this.redis = redis;
    this.mapper =
        jackson
            .getDefaultObjectMapper()
            .copy()
            .registerModule(DESERIALIZE_IMMUTABLE_BUILDER_NESTED);
    this.jobTypes = type -> Optional.empty();

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
    this.jobTypes = jobTypesMapper;
  }

  @Override
  protected String createQueue(String type, int priority) {
    redis.cmd().zadd(REDIS_KEY_PRIORITIES + type, priority, String.valueOf(priority));

    return REDIS_KEY_QUEUE + type + ":" + priority;
  }

  @Override
  protected Set<String> getTypes() {
    return redis.cmd().keys(REDIS_KEY_PRIORITIES + "*").stream()
        .map(key -> key.substring(REDIS_KEY_PRIORITIES.length()))
        .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
  }

  @Override
  protected Set<Integer> getPriorities(String type) {
    List<String> priorities = redis.cmd().zrevrange(REDIS_KEY_PRIORITIES + type, 0, -1);

    return new LinkedHashSet<>(priorities.stream().map(Integer::parseInt).toList());
  }

  @Override
  protected void updateJob(Job job) {
    try {
      redis.json().jsonSet(REDIS_KEY_JOB + job.getId(), mapper.writeValueAsString(job));
    } catch (Throwable e) {
      throw new IllegalStateException("Failed to serialize job to JSON: " + job.getId(), e);
    }
  }

  @Override
  public void updateJob(Job job, int progressDelta) {
    redis.json().jsonNumIncrBy(REDIS_KEY_JOB + job.getId(), Path2.of("$.current"), progressDelta);
    redis
        .json()
        .jsonSet(
            REDIS_KEY_JOB + job.getId(), Path2.of("$.updatedAt"), Instant.now().getEpochSecond());
  }

  @Override
  protected void updateJobSet(JobSet jobSet) {
    try {
      redis.json().jsonSet(REDIS_KEY_SET + jobSet.getId(), mapper.writeValueAsString(jobSet));
    } catch (Throwable e) {
      throw new IllegalStateException("Failed to serialize job set to JSON: " + jobSet.getId(), e);
    }
  }

  @Override
  public void startJobSet(JobSet jobSet) {
    redis
        .json()
        .jsonSet(
            REDIS_KEY_SET + jobSet.getId(),
            Path2.of("$.startedAt"),
            Instant.now().getEpochSecond());
  }

  @Override
  public void initJobSet(JobSet jobSet, int progressDelta, Map<String, Object> detailParameters) {
    Map<String, Object> jsonPathUpdates = new LinkedHashMap<>();
    jsonPathUpdates.put("$.total", progressDelta);

    JobSetDetails details = getJobSetDetails(JobSetDetails.class, jobSet);
    jsonPathUpdates.putAll(details.initJson(detailParameters));

    applyJsonPaths(jobSet.getId(), jsonPathUpdates);
  }

  @Override
  public void updateJobSet(JobSet jobSet, int progressDelta, Map<String, Object> detailParameters) {
    Map<String, Object> jsonPathUpdates = new LinkedHashMap<>();
    jsonPathUpdates.put("$.current", progressDelta);
    jsonPathUpdates.put("$.updatedAt", Instant.now().getEpochSecond());

    JobSetDetails details = getJobSetDetails(JobSetDetails.class, jobSet);
    jsonPathUpdates.putAll(details.updateJson(detailParameters));

    applyJsonPaths(jobSet.getId(), jsonPathUpdates);
  }

  @Override
  protected Optional<JobSet> getJobSet(String setId) {
    String jobSetJson = redis.json().jsonGetAsPlainString(REDIS_KEY_SET + setId, Path.ROOT_PATH);

    if (Objects.isNull(jobSetJson)) {
      return Optional.empty();
    }

    try {
      JobSet job = mapper.readValue(jobSetJson, JobSet.class);

      return Optional.ofNullable(job);
    } catch (Throwable e) {
      throw new IllegalStateException("Failed to deserialize job set from JSON: " + setId, e);
    }
  }

  @Override
  protected void queueJob(Job job, boolean untake) {
    String queue = getQueue(job.getType(), job.getPriority());
    updateJob(job);

    if (untake) {
      // NOPMD - TODO: use a transaction here
      redis.cmd().lrem(REDIS_KEY_TAKEN, 1, job.getId());
      redis.cmd().rpush(queue, job.getId());
    } else {
      redis.cmd().lpush(queue, job.getId());
    }
  }

  @Override
  protected Job resetJob(Job job, Optional<JobSet> jobSet) {
    if (jobSet.isPresent()) {
      jobSet.get().update(-(job.getCurrent().get()));
      JobSetDetails details = getJobSetDetails(JobSetDetails.class, jobSet.get());
      details.reset(job);
      updateJobSet(jobSet.get().with(details));
    }

    return job.reset();
  }

  @Override
  protected Job startJob(Job job, String executor) {
    Job startedJob = job.started(executor);

    updateJob(startedJob);

    return startedJob;
  }

  @Override
  protected Job failJob(Job job, String error) {
    Job failedJob = job.failed(error);

    updateJob(failedJob);

    redis.cmd().rpush(REDIS_KEY_FAILED, job.getId());

    return failedJob;
  }

  @Override
  protected Job doneJob(Job job) {
    Job doneJob = job.done();

    redis.json().jsonDel(REDIS_KEY_JOB + doneJob.getId());

    return doneJob;
  }

  @Override
  protected Optional<Job> takeJob(String queue) {
    String jobId =
        redis.cmd().lmove(queue, REDIS_KEY_TAKEN, ListDirection.RIGHT, ListDirection.LEFT);

    if (Objects.nonNull(jobId)) {
      return getJob(jobId);
    }

    return Optional.empty();
  }

  @Override
  protected Optional<Job> untakeJob(String jobId) {
    long count = redis.cmd().lrem(REDIS_KEY_TAKEN, 1, jobId);

    if (count > 0) {
      return getJob(jobId);
    }

    return Optional.empty();
  }

  @Override
  protected List<? extends BaseJob> onJobFinished(Job job, JobSet jobSet) {
    List<? extends BaseJob> followUps = jobSet.done(job);

    redis.json().jsonDel(REDIS_KEY_JOB + job.getId());

    return followUps;
  }

  @Override
  protected List<Job> getJobsInQueue(String queue) {
    List<String> jobIds = redis.cmd().lrange(queue, 0, -1);
    List<Job> jobs = new ArrayList<>();

    for (String jobId : jobIds) {
      Optional<Job> job = getJob(jobId);

      job.ifPresent(jobs::add);
    }

    return jobs;
  }

  @Override
  protected void notifyObservers(String type) {
    redis.pubsub().publish(REDIS_KEY_NOTIFICATIONS, type);
  }

  @Override
  public void onPush(Consumer<String> callback) {
    redis.pubsub().subscribe(REDIS_KEY_NOTIFICATIONS, callback);
  }

  @Override
  public boolean doneSet(String jobSetId) {
    long count = redis.json().jsonDel(REDIS_KEY_SET + jobSetId);

    return count > 0;
  }

  @Override
  public boolean error(String jobId, String error, boolean retry) {
    // NOPMD - TODO: retry logic
    return false;
  }

  @Override
  public Collection<JobSet> getSets() {
    Set<String> jobSetIds = redis.cmd().keys(REDIS_KEY_SET + "*");

    return jobSetIds.stream()
        .map(id -> id.substring(REDIS_KEY_SET.length()))
        .map(this::getJobSet)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  @Override
  protected List<String> getTakenIds() {
    return redis.cmd().lrange(REDIS_KEY_TAKEN, 0, -1);
  }

  @Override
  protected List<String> getFailedIds() {
    return redis.cmd().lrange(REDIS_KEY_FAILED, 0, -1);
  }

  @Override
  protected Optional<Job> getJob(String jobId) {
    String jobJson = redis.json().jsonGetAsPlainString(REDIS_KEY_JOB + jobId, Path.ROOT_PATH);

    if (Objects.isNull(jobJson)) {
      return Optional.empty();
    }

    try {
      Job job = mapper.readValue(jobJson, Job.class);

      return Optional.ofNullable(job);
    } catch (Throwable e) {
      throw new IllegalStateException("Failed to deserialize job from JSON: " + jobId, e);
    }
  }

  private Object unpackDetails(Job job) {
    if (Objects.nonNull(job)
        && Objects.nonNull(job.getDetails())
        && job.getDetails() instanceof Map
        && !((Map<?, ?>) job.getDetails()).isEmpty()) {
      try {
        Object details =
            mapper.readValue(
                mapper.writeValueAsBytes(job.getDetails()),
                jobTypes.apply(job.getType()).orElseThrow());

        if (details instanceof JobDetails) {
          return details;
        }

      } catch (IOException e) {
        throw new IllegalStateException(
            "Failed to convert job details to target type: " + job.getType(), e);
      }
    }

    return job.getDetails();
  }

  private Object unpackSetDetails(JobSet jobSet) {
    if (Objects.nonNull(jobSet)
        && Objects.nonNull(jobSet.getDetails())
        && jobSet.getDetails() instanceof Map
        && !((Map<?, ?>) jobSet.getDetails()).isEmpty()) {
      try {
        Object details =
            mapper.readValue(
                mapper.writeValueAsBytes(jobSet.getDetails()),
                jobTypes.apply(jobSet.getType()).orElseThrow());

        if (details instanceof JobSetDetails) {
          return details;
        }

      } catch (IOException e) {
        throw new IllegalStateException(
            "Failed to convert job set details to target type: " + jobSet.getType(), e);
      }
    }

    return jobSet.getDetails();
  }

  @Override
  public <T> T getJobDetails(Class<T> detailsType, Job job) {
    return detailsType.cast(unpackDetails(job));
  }

  @Override
  public <T> T getJobSetDetails(Class<T> detailsType, JobSet jobSet) {
    return detailsType.cast(unpackSetDetails(jobSet));
  }

  private void applyJsonPaths(String jobSetId, Map<String, Object> jsonPathUpdates) {
    for (Map.Entry<String, Object> entry : jsonPathUpdates.entrySet()) {
      if (entry.getValue() instanceof Integer) {
        redis
            .json()
            .jsonNumIncrBy(
                REDIS_KEY_SET + jobSetId, Path2.of(entry.getKey()), (Integer) entry.getValue());
        continue;
      }
      redis.json().jsonSet(REDIS_KEY_SET + jobSetId, Path2.of(entry.getKey()), entry.getValue());
    }
  }
}
