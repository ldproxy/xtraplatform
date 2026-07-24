/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.jobs.domain.ImmutableJobV2Impl;
import de.ii.xtraplatform.jobs.domain.JobQueueV2;
import de.ii.xtraplatform.jobs.domain.JobV2;
import de.ii.xtraplatform.jobs.domain.JobV2Impl;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.security.InvalidParameterException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

@Singleton
@AutoBind
public class JobQueueV2Impl implements JobQueueV2 {

  private final Map<String, JobV2Impl> jobs = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);
  private final Map<String, Function<Map<String, Object>, Map<String, Object>>> processesMap =
      Map.of(
          "AnswerProcess",
          this::answerProcess,
          "EchoProcess",
          this::echoProcess,
          "AdditionProcess",
          this::additionProcess);

  @Inject
  public JobQueueV2Impl() {}

  @Override
  public JobV2 createJob(String type, Map<String, Object> inputs) {
    return createJob(type, inputs, null);
  }

  @Override
  public JobV2 createJob(String type, Map<String, Object> inputs, Object details) {
    return new ImmutableJobV2Impl.Builder()
        .type(type)
        .inputs(inputs)
        .total(new AtomicInteger(100))
        .details(details)
        .build();
  }

  @Override
  public JobV2 get(String jobId) {
    return jobs.get(jobId);
  }

  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  private void doPush(JobV2 job, Consumer<JobV2> onChange) {
    JobV2Impl jobV2 = (JobV2Impl) job;
    jobs.put(job.getId(), jobV2);

    // Start job
    scheduler.schedule(
        () -> {
          JobV2Impl newJob =
              new ImmutableJobV2Impl.Builder()
                  .from(jobV2)
                  .startedAt(new AtomicLong(Instant.now().getEpochSecond()))
                  .updatedAt(new AtomicLong(Instant.now().getEpochSecond()))
                  .status(JobV2.Status.RUNNING)
                  .build();
          jobs.put(job.getId(), newJob);

          onChange.accept(newJob);
        },
        1,
        TimeUnit.SECONDS);

    // Update job
    scheduler.schedule(
        () -> {
          JobV2Impl newJob =
              new ImmutableJobV2Impl.Builder()
                  .from(jobV2)
                  .updatedAt(new AtomicLong(Instant.now().getEpochSecond()))
                  .current(new AtomicInteger(60))
                  .build();
          jobs.put(job.getId(), newJob);

          onChange.accept(newJob);
        },
        5,
        TimeUnit.SECONDS);

    // Finished job
    scheduler.schedule(
        () -> {
          try {
            Map<String, Object> results = processesMap.get(job.getType()).apply(job.getInputs());

            JobV2Impl newJob =
                new ImmutableJobV2Impl.Builder()
                    .from(jobV2)
                    .updatedAt(new AtomicLong(Instant.now().getEpochSecond()))
                    .finishedAt(new AtomicLong(Instant.now().getEpochSecond()))
                    .current(new AtomicInteger(100))
                    .status(JobV2.Status.SUCCESSFUL)
                    .outputs(results)
                    .build();
            jobs.put(job.getId(), newJob);
            onChange.accept(newJob);

          } catch (Throwable e) {
            JobV2Impl newJob =
                new ImmutableJobV2Impl.Builder()
                    .from(jobV2)
                    .updatedAt(new AtomicLong(Instant.now().getEpochSecond()))
                    .finishedAt(new AtomicLong(Instant.now().getEpochSecond()))
                    .current(new AtomicInteger(100))
                    .status(JobV2.Status.FAILED)
                    .errors(List.of(e.getMessage()))
                    .build();
            jobs.put(job.getId(), newJob);
            onChange.accept(newJob);
          }
        },
        10,
        TimeUnit.SECONDS);
  }

  @Override
  public CompletableFuture<JobV2> push(JobV2 job) {
    return push(job, j -> {});
  }

  @Override
  public CompletableFuture<JobV2> push(JobV2 job, Consumer<JobV2> onChange) {

    CompletableFuture<JobV2> future = new CompletableFuture<>();

    Consumer<JobV2> onChangeWrapper =
        j -> {
          onChange.accept(j);
          if (j.getStatus() == JobV2.Status.SUCCESSFUL || j.getStatus() == JobV2.Status.FAILED) {
            future.complete(j);
          }
        };

    doPush(job, onChangeWrapper);

    return future;
  }

  /***
   * Functions for faking the job queue
   * ToDo Remove after integrating the job queue
   ***/

  private Map<String, Object> echoProcess(Map<String, Object> inputs) {
    return inputs;
  }

  @SuppressWarnings("PMD.UnusedFormalParameter")
  private Map<String, Object> answerProcess(Map<String, Object> inputs) {
    return Map.of("answer", 42);
  }

  private Map<String, Object> additionProcess(Map<String, Object> inputs) {
    if (!inputs.containsKey("firstAddend") || !inputs.containsKey("secondAddend")) {
      throw new InvalidParameterException("Wrong inputs");
    }

    int firstAddend = (Integer) inputs.get("firstAddend");
    int secondAddend = (Integer) inputs.get("secondAddend");

    return Map.of("sum", firstAddend + secondAddend);
  }
}
