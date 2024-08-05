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
import de.ii.xtraplatform.jobs.domain.BaseJob;
import de.ii.xtraplatform.jobs.domain.Job;
import de.ii.xtraplatform.jobs.domain.JobQueue;
import de.ii.xtraplatform.jobs.domain.JobSet;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class JobQueueSimple implements JobQueue {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobQueueSimple.class);

  private final Map<String, JobSet> jobSets;
  private final Map<String, Deque<Job>> queues;
  private final Queue<Job> openQueue;
  private final List<Job> takenQueue;
  private final List<Job> errorQueue;

  @Inject
  JobQueueSimple(AppContext appContext) {
    // TODO: if enabled, start embedded queue
    this.jobSets = new ConcurrentHashMap<>();
    this.queues = new ConcurrentHashMap<>();
    this.openQueue = new ArrayDeque<>();
    this.takenQueue = new CopyOnWriteArrayList<>();
    this.errorQueue = new CopyOnWriteArrayList<>();

    // TODO: housekeeping
  }

  @Override
  public synchronized void push(BaseJob job, boolean untake) {
    if (!queues.containsKey(job.getType())) {
      queues.put(job.getType(), new ArrayDeque<>());
    }
    if (job instanceof Job) {
      if (untake) {
        takenQueue.remove(job);
        queues.get(job.getType()).addFirst(((Job) job).reset());
      } else {
        queues.get(job.getType()).add((Job) job);
      }
    } else if (job instanceof JobSet) {
      jobSets.put(job.getId(), (JobSet) job);

      if (((JobSet) job).getSetup().isPresent()) {
        push(((JobSet) job).getSetup().get());
      }
    } else {
      throw new IllegalArgumentException("Unknown job type: " + job.getClass());
    }
  }

  @Override
  public synchronized Optional<Job> take(String type, String executor) {
    if (!queues.containsKey(type)) {
      queues.put(type, new ArrayDeque<>());
    }
    if (!queues.get(type).isEmpty()) {
      Job job = queues.get(type).remove().started(executor);
      takenQueue.add(job);

      return Optional.of(job);
    }

    return Optional.empty();
  }

  @Override
  public synchronized boolean done(String jobId) {
    Optional<Job> job =
        takenQueue.stream().filter(job1 -> Objects.equals(job1.getId(), jobId)).findFirst();

    if (job.isPresent()) {
      if (job.get().getPartOf().isPresent()) {
        String setId = job.get().getPartOf().get();

        if (jobSets.containsKey(setId)) {
          List<BaseJob> followUps = jobSets.get(setId).done(job.get());
          followUps.forEach(this::push);
          // TODO: if done, mark for removal
        }
      }

      takenQueue.remove(job.get());

      job.get().getFollowUps().forEach(this::push);

      return true;
    }

    return false;
  }

  @Override
  public synchronized boolean doneSet(String jobSetId) {
    return jobSets.remove(jobSetId) != null;
  }

  @Override
  public synchronized boolean error(String jobId, String error, boolean retry) {
    Optional<Job> job =
        takenQueue.stream().filter(job1 -> Objects.equals(job1.getId(), jobId)).findFirst();

    if (job.isPresent()) {
      takenQueue.remove(job.get());

      if (retry) {
        int retries = job.get().getRetries().orElse(0);
        if (retries < 3) {
          push(job.get().retry(error), true);

          return true;
        }
      }

      errorQueue.add(job.get().failed(error));

      if (job.get().getPartOf().isPresent()) {
        String setId = job.get().getPartOf().get();

        if (jobSets.containsKey(setId)) {
          List<BaseJob> followUps = jobSets.get(setId).done(job.get());
          followUps.forEach(this::push);
          // TODO: if done, mark for removal
        }
      }
    }

    return false;
  }

  @Override
  public Collection<JobSet> getSets() {
    return jobSets.values();
  }

  @Override
  public Map<String, Deque<Job>> getOpen() {
    return queues;
  }

  @Override
  public Collection<Job> getTaken() {
    return takenQueue;
  }

  @Override
  public Collection<Job> getFailed() {
    return errorQueue;
  }

  @Override
  public JobSet getSet(String setId) {
    return jobSets.get(setId);
  }
}
