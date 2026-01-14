/*
 * Copyright 2025 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.domain;

import de.ii.xtraplatform.base.domain.resiliency.AbstractVolatileComposed;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("PMD.TooManyMethods")
public abstract class AbstractJobQueueBackend<T> extends AbstractVolatileComposed
    implements JobQueueBackend {

  private final Map<String, Set<Integer>> priorities;
  private final Map<String, Map<Integer, T>> queues;

  // Pre-create comparator and avoid object creation in checkQueue
  private static final Comparator<Integer> REVERSE_ORDER = Comparator.<Integer>reverseOrder();

  protected AbstractJobQueueBackend(VolatileRegistry volatileRegistry) {
    super(volatileRegistry);

    this.priorities = new ConcurrentHashMap<>();
    this.queues = new ConcurrentHashMap<>();
  }

  protected abstract T createQueue(String type, int priority);

  protected abstract Optional<JobSet> getJobSet(String setId);

  protected abstract void updateJobSet(JobSet jobSet);

  protected abstract void queueJob(Job job, boolean untake);

  protected abstract Optional<Job> getJob(String jobId);

  protected abstract void updateJob(Job job);

  protected abstract Job resetJob(Job job, Optional<JobSet> jobSet);

  protected abstract Job startJob(Job job, String executor);

  protected abstract Job failJob(Job job, String error);

  protected abstract Job doneJob(Job job);

  protected abstract Optional<Job> takeJob(T queue);

  protected abstract Optional<Job> untakeJob(String jobId);

  protected abstract List<? extends BaseJob> onJobFinished(Job job, JobSet jobSet);

  protected abstract List<Job> getJobsInQueue(T queue);

  protected abstract void notifyObservers(String type);

  @Override
  public void push(BaseJob job, boolean untake) {
    if (job instanceof Job) {
      if (untake) {
        Optional<JobSet> jobSet = ((Job) job).getPartOf().flatMap(this::getJobSet);
        Job freshJob = resetJob((Job) job, jobSet);

        queueJob(freshJob, true);
      } else {
        queueJob((Job) job, false);
      }

      notifyObservers(job.getType());
    } else if (job instanceof JobSet) {
      updateJobSet((JobSet) job);

      if (((JobSet) job).getSetup().isPresent()) {
        push(((JobSet) job).getSetup().get(), false);
      }
    } else {
      throw new IllegalArgumentException("Unknown job type: " + job.getClass());
    }
  }

  @Override
  public Optional<Job> take(String type, String executor) {
    for (int priority : getPriorities(type)) {
      T queue = getQueue(type, priority);
      Optional<Job> job = takeJob(queue);

      if (job.isPresent()) {
        return Optional.of(startJob(job.get(), executor));
      }
    }

    return Optional.empty();
  }

  @Override
  public boolean done(String jobId) {
    Optional<Job> job = untakeJob(jobId);

    if (job.isPresent()) {
      Job doneJob = doneJob(job.get());
      onJobFinished(doneJob);

      doneJob.getFollowUps().forEach(followUp -> push(followUp, false));

      return true;
    }

    return false;
  }

  @Override
  public boolean error(String jobId, String error, boolean retry) {
    Optional<Job> job = untakeJob(jobId);

    if (job.isPresent()) {
      if (retry) {
        int retries = job.get().getRetries().orElse(0);
        if (retries < 3) {
          queueJob(job.get().retry(error), true);

          return true;
        }
      }

      Job failedJob = failJob(job.get(), error);
      onJobFinished(failedJob);
    }

    return false;
  }

  @Override
  public JobSet getSet(String setId) {
    return getJobSet(setId).orElse(null);
  }

  @Override
  public Map<String, Map<Integer, List<Job>>> getOpen() {
    Set<String> priorityTypes = getTypes();

    return priorityTypes.stream()
        .collect(
            LinkedHashMap::new,
            (map, type) -> {
              Set<Integer> priorities = getPriorities(type);
              Map<Integer, List<Job>> priorityMap =
                  priorities.stream()
                      .collect(
                          LinkedHashMap::new,
                          (pMap, priority) -> {
                            T queue = getQueue(type, priority);
                            pMap.put(priority, getJobsInQueue(queue));
                          },
                          Map::putAll);
              map.put(type, priorityMap);
            },
            Map::putAll);
  }

  protected abstract List<String> getTakenIds();

  protected abstract List<String> getFailedIds();

  @Override
  public Collection<Job> getTaken() {
    List<String> takenIds = getTakenIds();
    return takenIds.stream()
        .map(this::getJob)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  @Override
  public Collection<Job> getFailed() {
    List<String> failedIds = getFailedIds();
    return failedIds.stream()
        .map(this::getJob)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  protected final T getQueue(String type, int priority) {
    checkQueue(type, priority);

    return queues.get(type).get(priority);
  }

  protected Set<String> getTypes() {
    return priorities.keySet();
  }

  protected Set<Integer> getPriorities(String type) {
    return priorities.getOrDefault(type, Set.of());
  }

  private void checkQueue(String type, int priority) {
    // Use computeIfAbsent to avoid checking and creating separately
    Set<Integer> typePriorities =
        priorities.computeIfAbsent(
            type,
            k -> {
              queues.put(k, new ConcurrentHashMap<>());
              return new TreeSet<>(REVERSE_ORDER);
            });

    // Use computeIfAbsent for queue creation too
    Map<Integer, T> typeQueues = queues.get(type);
    if (!typePriorities.contains(priority)) {
      typePriorities.add(priority);
      typeQueues.computeIfAbsent(priority, p -> createQueue(type, p));
    }
  }

  private void onJobFinished(Job job) {
    if (job.getPartOf().isPresent()) {
      String setId = job.getPartOf().get();
      Optional<JobSet> jobSet = getJobSet(setId);

      if (jobSet.isPresent()) {
        // TODO: if done, mark for removal
        List<? extends BaseJob> jobSetFollowUps = onJobFinished(job, jobSet.get());
        jobSetFollowUps.forEach(followUp -> push(followUp, false));
      }
    }
  }
}
