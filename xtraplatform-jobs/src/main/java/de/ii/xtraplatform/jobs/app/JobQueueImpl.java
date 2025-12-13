/*
 * Copyright 2025 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.resiliency.AbstractVolatileComposed;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.jobs.domain.BaseJob;
import de.ii.xtraplatform.jobs.domain.Job;
import de.ii.xtraplatform.jobs.domain.JobQueue;
import de.ii.xtraplatform.jobs.domain.JobQueueBackend;
import de.ii.xtraplatform.jobs.domain.JobSet;
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
@AutoBind(interfaces = {JobQueue.class})
public class JobQueueImpl extends AbstractVolatileComposed implements JobQueue {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobQueueImpl.class);

  private final JobQueueBackend backend;

  @Inject
  JobQueueImpl(VolatileRegistry volatileRegistry, Set<JobQueueBackend> backends) {
    super(volatileRegistry);

    // LOGGER.info("JOBS BACKENDS: {}", backends);
    this.backend =
        backends.stream()
            .filter(JobQueueBackend::isEnabled)
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("No JobQueueBackend implementation found"));

    if (volatileRegistry != null) {
      onVolatileStart();

      addSubcomponent(backend);

      onVolatileStarted();
    }
  }

  @Override
  public void setJobTypes(Function<String, Optional<? extends Class<?>>> jobTypesMapper) {
    backend.setJobTypes(jobTypesMapper);
  }

  @Override
  public void push(BaseJob job, boolean untake) {
    backend.push(job, untake);
  }

  @Override
  public void onPush(Consumer<String> callback) {
    backend.onPush(callback);
  }

  @Override
  public Optional<Job> take(String type, String executor) {
    return backend.take(type, executor);
  }

  @Override
  public boolean done(String jobId) {
    return backend.done(jobId);
  }

  @Override
  public boolean doneSet(String jobSetId) {
    return backend.doneSet(jobSetId);
  }

  @Override
  public boolean error(String jobId, String error, boolean retry) {
    return backend.error(jobId, error, retry);
  }

  @Override
  public Collection<JobSet> getSets() {
    return backend.getSets();
  }

  @Override
  public Map<String, Map<Integer, List<Job>>> getOpen() {
    return backend.getOpen();
  }

  @Override
  public Collection<Job> getTaken() {
    return backend.getTaken();
  }

  @Override
  public Collection<Job> getFailed() {
    return backend.getFailed();
  }

  @Override
  public JobSet getSet(String setId) {
    return backend.getSet(setId);
  }

  @Override
  public void updateJob(Job job, int progressDelta) {
    backend.updateJob(job, progressDelta);
  }

  @Override
  public void startJobSet(JobSet jobSet) {
    backend.startJobSet(jobSet);
  }

  @Override
  public void initJobSet(JobSet jobSet, int total, Map<String, Object> detailParameters) {
    backend.initJobSet(jobSet, total, detailParameters);
  }

  @Override
  public void updateJobSet(JobSet jobSet, int progressDelta, Map<String, Object> detailParameters) {
    backend.updateJobSet(jobSet, progressDelta, detailParameters);
  }

  @Override
  public <T> T getJobDetails(Class<T> detailsType, Job job) {
    return backend.getJobDetails(detailsType, job);
  }

  @Override
  public <T> T getJobSetDetails(Class<T> detailsType, JobSet jobSet) {
    return backend.getJobSetDetails(detailsType, jobSet);
  }
}
