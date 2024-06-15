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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
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

  @Inject
  JobQueueSimple(AppContext appContext) {
    // TODO: if enabled, start embedded queue
    this.jobSets = new ConcurrentHashMap<>();
    this.queues = new ConcurrentHashMap<>();
    this.openQueue = new ArrayDeque<>();
    this.takenQueue = new ArrayList<>();

    // TODO: housekeeping
  }

  @Override
  public synchronized void push(BaseJob job, boolean head) {
    if (!queues.containsKey(job.getType())) {
      queues.put(job.getType(), new ArrayDeque<>());
    }
    if (job instanceof Job) {
      if (head) {
        queues.get(job.getType()).addFirst((Job) job);
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
      Job job = queues.get(type).remove();
      takenQueue.add(job.started(executor));

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
  public synchronized boolean error(String jobId, String error) {
    // TODO: retry logic
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
  public JobSet getSet(String setId) {
    return jobSets.get(setId);
  }
}
