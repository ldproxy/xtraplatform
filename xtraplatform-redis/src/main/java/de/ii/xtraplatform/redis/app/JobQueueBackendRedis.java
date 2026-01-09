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
public class JobQueueBackendRedis extends AbstractJobQueueBackend<String>
    implements JobQueueBackend {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobQueueBackendRedis.class);
  private static final List<Integer> INITIAL_LEVELS =
      IntStream.range(0, 24).map(i -> -1).boxed().toList();

  private final boolean enabled;
  private final Redis redis;
  private final ObjectMapper mapper;
  private Function<String, Optional<? extends Class<?>>> jobTypes;

  @Inject
  JobQueueBackendRedis(
      AppContext appContext, Jackson jackson, VolatileRegistry volatileRegistry, Redis redis) {
    super(volatileRegistry);

    // TODO: housekeeping might check taken list using RPOPLPUSH with same source and destination
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
    redis.cmd().zadd("xtraplatform:jobs:priorities:" + type, priority, String.valueOf(priority));

    return "xtraplatform:jobs:queue:" + type + ":" + priority;
  }

  @Override
  protected Set<String> getTypes() {
    return redis.cmd().keys("xtraplatform:jobs:priorities:*").stream()
        .map(key -> key.substring("xtraplatform:jobs:priorities:".length()))
        .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
  }

  @Override
  protected Set<Integer> getPriorities(String type) {
    List<String> priorities = redis.cmd().zrevrange("xtraplatform:jobs:priorities:" + type, 0, -1);

    return new LinkedHashSet<>(priorities.stream().map(Integer::parseInt).toList());
  }

  @Override
  protected void updateJob(Job job) {
    try {
      redis.json().jsonSet("xtraplatform:jobs:job:" + job.getId(), mapper.writeValueAsString(job));
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void updateJob(Job job, int progressDelta) {
    redis
        .json()
        .jsonNumIncrBy(
            "xtraplatform:jobs:job:" + job.getId(), Path2.of("$.current"), progressDelta);
    redis
        .json()
        .jsonSet(
            "xtraplatform:jobs:job:" + job.getId(),
            Path2.of("$.updatedAt"),
            Instant.now().getEpochSecond());
  }

  @Override
  protected void updateJobSet(JobSet jobSet) {
    try {
      redis
          .json()
          .jsonSet("xtraplatform:jobs:set:" + jobSet.getId(), mapper.writeValueAsString(jobSet));
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void startJobSet(JobSet jobSet) {
    redis
        .json()
        .jsonSet(
            "xtraplatform:jobs:set:" + jobSet.getId(),
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
    String jobSetJson =
        redis.json().jsonGetAsPlainString("xtraplatform:jobs:set:" + setId, Path.ROOT_PATH);

    if (Objects.isNull(jobSetJson)) {
      return Optional.empty();
    }

    try {
      JobSet job = mapper.readValue(jobSetJson, JobSet.class);

      return Optional.ofNullable(job);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void queueJob(Job job, boolean untake) {
    String queue = getQueue(job.getType(), job.getPriority());
    updateJob(job);

    if (untake) {
      // TODO: use a transaction here
      redis.cmd().lrem("xtraplatform:jobs:taken", 1, job.getId());
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

    redis.cmd().rpush("xtraplatform:jobs:failed", job.getId());

    return failedJob;
  }

  @Override
  protected Job doneJob(Job job) {
    Job doneJob = job.done();

    redis.json().jsonDel("xtraplatform:jobs:job:" + doneJob.getId());

    return doneJob;
  }

  @Override
  protected Optional<Job> takeJob(String queue) {
    String jobId =
        redis
            .cmd()
            .lmove(queue, "xtraplatform:jobs:taken", ListDirection.RIGHT, ListDirection.LEFT);

    if (Objects.nonNull(jobId)) {
      return getJob(jobId);
    }

    return Optional.empty();
  }

  @Override
  protected Optional<Job> untakeJob(String jobId) {
    long count = redis.cmd().lrem("xtraplatform:jobs:taken", 1, jobId);

    if (count > 0) {
      return getJob(jobId);
    }

    return Optional.empty();
  }

  @Override
  protected List<? extends BaseJob> onJobFinished(Job job, JobSet jobSet) {
    List<? extends BaseJob> followUps = jobSet.done(job);

    redis.json().jsonDel("xtraplatform:jobs:job:" + job.getId());

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
    redis.pubsub().publish("xtraplatform:jobs:notifications", type);
  }

  @Override
  public void onPush(Consumer<String> callback) {
    redis.pubsub().subscribe("xtraplatform:jobs:notifications", callback);
  }

  @Override
  public boolean doneSet(String jobSetId) {
    long count = redis.json().jsonDel("xtraplatform:jobs:set:" + jobSetId);

    return count > 0;
  }

  @Override
  public boolean error(String jobId, String error, boolean retry) {
    // TODO: retry logic
    return false;
  }

  @Override
  public Collection<JobSet> getSets() {
    Set<String> jobSetIds = redis.cmd().keys("xtraplatform:jobs:set:*");

    return jobSetIds.stream()
        .map(id -> id.substring("xtraplatform:jobs:set:".length()))
        .map(this::getJobSet)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  @Override
  protected List<String> getTakenIds() {
    return redis.cmd().lrange("xtraplatform:jobs:taken", 0, -1);
  }

  @Override
  protected List<String> getFailedIds() {
    return redis.cmd().lrange("xtraplatform:jobs:failed", 0, -1);
  }

  @Override
  protected Optional<Job> getJob(String jobId) {
    String jobJson =
        redis.json().jsonGetAsPlainString("xtraplatform:jobs:job:" + jobId, Path.ROOT_PATH);

    if (Objects.isNull(jobJson)) {
      return Optional.empty();
    }

    try {
      Job job = mapper.readValue(jobJson, Job.class);

      return Optional.ofNullable(job);
    } catch (Throwable e) {
      throw new RuntimeException(e);
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
        throw new RuntimeException(e);
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
        throw new RuntimeException(e);
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
                "xtraplatform:jobs:set:" + jobSetId,
                Path2.of(entry.getKey()),
                (Integer) entry.getValue());
        continue;
      }
      redis
          .json()
          .jsonSet("xtraplatform:jobs:set:" + jobSetId, Path2.of(entry.getKey()), entry.getValue());
    }
  }
}
