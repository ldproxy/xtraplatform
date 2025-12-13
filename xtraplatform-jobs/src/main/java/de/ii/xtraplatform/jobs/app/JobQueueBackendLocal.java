/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.JobsConfiguration.QUEUE;
import de.ii.xtraplatform.base.domain.resiliency.Volatile2;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.jobs.domain.AbstractJobQueueBackend;
import de.ii.xtraplatform.jobs.domain.BaseJob;
import de.ii.xtraplatform.jobs.domain.Job;
import de.ii.xtraplatform.jobs.domain.JobQueueBackend;
import de.ii.xtraplatform.jobs.domain.JobSet;
import de.ii.xtraplatform.jobs.domain.JobSet.JobSetDetails;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind(interfaces = JobQueueBackend.class)
public class JobQueueBackendLocal extends AbstractJobQueueBackend<Deque<String>>
    implements JobQueueBackend {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobQueueBackendLocal.class);

  private final boolean enabled;
  private final Map<String, JobSet> jobSets;
  private final Map<String, Job> jobs;
  private final List<String> takenQueue;
  private final List<String> errorQueue;
  private final List<Consumer<String>> observers;

  @Inject
  JobQueueBackendLocal(AppContext appContext, VolatileRegistry volatileRegistry) {
    super(volatileRegistry);

    this.enabled = appContext.getConfiguration().getJobs().getQueue() == QUEUE.LOCAL;
    this.jobSets = new ConcurrentHashMap<>();
    this.jobs = new ConcurrentHashMap<>();
    this.takenQueue = new CopyOnWriteArrayList<>();
    this.errorQueue = new CopyOnWriteArrayList<>();
    this.observers = new CopyOnWriteArrayList<>();

    if (volatileRegistry != null) {
      onVolatileStart();
      addSubcomponent(Volatile2.available("app/jobs/local"));
      onVolatileStarted();
    }
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setJobTypes(Function<String, Optional<? extends Class<?>>> jobTypesMapper) {}

  @Override
  protected Deque<String> createQueue(String type, int priority) {
    return new ArrayDeque<>();
  }

  @Override
  protected void updateJob(Job job) {
    jobs.put(job.getId(), job);
  }

  @Override
  public void updateJob(Job job, int progressDelta) {
    job.update(progressDelta);
  }

  @Override
  protected void updateJobSet(JobSet jobSet) {
    jobSets.put(jobSet.getId(), jobSet);
  }

  @Override
  public void startJobSet(JobSet jobSet) {
    jobSet.start();
  }

  @Override
  public void initJobSet(JobSet jobSet, int total, Map<String, Object> detailParameters) {
    jobSet.init(total);
    if (jobSet.getDetails() instanceof JobSetDetails) {
      ((JobSetDetails) jobSet.getDetails()).init(detailParameters);
    }
  }

  @Override
  public void updateJobSet(JobSet jobSet, int progressDelta, Map<String, Object> detailParameters) {
    jobSet.update(progressDelta);
    if (jobSet.getDetails() instanceof JobSetDetails) {
      ((JobSetDetails) jobSet.getDetails()).update(detailParameters);
    }
  }

  @Override
  protected Optional<JobSet> getJobSet(String setId) {
    return Optional.ofNullable(jobSets.get(setId));
  }

  @Override
  protected Optional<Job> getJob(String jobId) {
    return Optional.ofNullable(jobs.get(jobId));
  }

  @Override
  protected void queueJob(Job job, boolean untake) {
    Deque<String> queue = getQueue(job.getType(), job.getPriority());
    updateJob(job);

    if (untake) {
      takenQueue.remove(job.getId());
      queue.addFirst(job.getId());
    } else {
      queue.add(job.getId());
    }
  }

  @Override
  protected Job resetJob(Job job, Optional<JobSet> jobSet) {
    if (jobSet.isPresent()) {
      jobSet.get().update(-(job.getCurrent().get()));
      getJobSetDetails(JobSetDetails.class, jobSet.get()).reset(job);
      updateJobSet(jobSet.get());
    }

    Job resettedJob = job.reset();
    updateJob(resettedJob);

    return resettedJob;
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
    errorQueue.add(failedJob.getId());

    return failedJob;
  }

  @Override
  protected Job doneJob(Job job) {
    Job doneJob = job.done();
    jobs.remove(doneJob.getId());

    return doneJob;
  }

  @Override
  protected Optional<Job> takeJob(Deque<String> queue) {
    if (!queue.isEmpty()) {
      String jobId = queue.remove();
      takenQueue.add(jobId);

      return getJob(jobId);
    }

    return Optional.empty();
  }

  @Override
  protected Optional<Job> untakeJob(String jobId) {
    Optional<String> id =
        takenQueue.stream().filter(takenId -> Objects.equals(takenId, jobId)).findFirst();

    if (id.isPresent()) {
      takenQueue.remove(id.get());

      return getJob(id.get());
    }

    return Optional.empty();
  }

  @Override
  protected List<? extends BaseJob> onJobFinished(Job job, JobSet jobSet) {
    return jobSet.done(job);
  }

  @Override
  protected List<Job> getJobsInQueue(Deque<String> queue) {
    return queue.stream().map(this::getJob).filter(Optional::isPresent).map(Optional::get).toList();
  }

  @Override
  protected void notifyObservers(String type) {
    LOGGER.debug("NOTIFY {}", type);
    observers.forEach(observer -> observer.accept(type));
  }

  @Override
  public void onPush(Consumer<String> callback) {
    this.observers.add(callback);
  }

  @Override
  public boolean doneSet(String jobSetId) {
    return jobSets.remove(jobSetId) != null;
  }

  @Override
  public Collection<JobSet> getSets() {
    return jobSets.values();
  }

  @Override
  protected List<String> getTakenIds() {
    return takenQueue;
  }

  @Override
  public List<String> getFailedIds() {
    return errorQueue;
  }

  @Override
  public <T> T getJobDetails(Class<T> detailsType, Job job) {
    return detailsType.cast(job.getDetails());
  }

  @Override
  public <T> T getJobSetDetails(Class<T> detailsType, JobSet jobSet) {
    return detailsType.cast(jobSet.getDetails());
  }
}
