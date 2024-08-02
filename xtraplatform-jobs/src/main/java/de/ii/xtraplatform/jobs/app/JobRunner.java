/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dagger.Lazy;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.jobs.domain.Job;
import de.ii.xtraplatform.jobs.domain.JobProcessor;
import de.ii.xtraplatform.jobs.domain.JobQueue;
import de.ii.xtraplatform.jobs.domain.JobResult;
import de.ii.xtraplatform.jobs.domain.JobSet;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.AmountFormats;

@Singleton
@AutoBind
public class JobRunner implements AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobRunner.class);

  private final JobQueue jobQueue;
  private final Lazy<Set<JobProcessor<?, ?>>> processors;
  private final ScheduledExecutorService polling;
  private final ExecutorService executor;
  private final String executorName;
  private final int maxThreads;
  private final Supplier<Integer> activeThreads;
  private final Map<String, AtomicInteger> jobSetConcurrency;
  private final Map<String, Integer> jobSetConcurrencyMax;

  @Inject
  JobRunner(AppContext appContext, JobQueue jobQueue, Lazy<Set<JobProcessor<?, ?>>> processors) {
    this.jobQueue = jobQueue;
    this.processors = processors;
    this.polling =
        MoreExecutors.getExitingScheduledExecutorService(
            (ScheduledThreadPoolExecutor)
                Executors.newScheduledThreadPool(
                    1, new ThreadFactoryBuilder().setNameFormat("jobs.poll-%d").build()));
    ThreadPoolExecutor threadPoolExecutor =
        (ThreadPoolExecutor)
            Executors.newFixedThreadPool(
                appContext.getConfiguration().getBackgroundTasks().getMaxThreads(),
                new ThreadFactoryBuilder().setNameFormat("jobs.exec-%d").build());
    this.executor = MoreExecutors.getExitingExecutorService(threadPoolExecutor);
    this.executorName = appContext.getInstanceName();
    this.maxThreads = threadPoolExecutor.getMaximumPoolSize();
    this.activeThreads = threadPoolExecutor::getActiveCount;

    this.jobSetConcurrency =
        new LinkedHashMap<>() {
          // delete the oldest entry if the map size exceeds 1023
          @Override
          protected boolean removeEldestEntry(Map.Entry<String, AtomicInteger> eldest) {
            return size() > 1023;
          }
        };
    this.jobSetConcurrencyMax =
        new LinkedHashMap<>() {
          // delete the oldest entry if the map size exceeds 1023
          @Override
          protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
            return size() > 1023;
          }
        };
  }

  @Override
  public CompletionStage<Void> onStart(boolean isStartupAsync) {
    // check for new jobs every second
    polling.scheduleAtFixedRate(
        () -> {
          for (JobProcessor<?, ?> processor : processors.get()) {
            while (activeThreads.get() < maxThreads) {
              Optional<Job> optionalJob = jobQueue.take(processor.getJobType(), executorName);

              if (optionalJob.isPresent()) {
                if (optionalJob.get().getPartOf().isPresent()) {
                  JobSet jobSet = jobQueue.getSet(optionalJob.get().getPartOf().get());

                  if (jobSet.getEntity().isPresent()) {
                    LogContext.put(LogContext.CONTEXT.SERVICE, jobSet.getEntity().get());
                  }

                  if (shouldSuspend(jobSet, processor)) {
                    if (logJobsTrace()) {
                      LOGGER.trace(
                          MARKER.JOBS,
                          "Max concurrency reached, suspending job set ({})",
                          optionalJob.get().getPartOf().get());
                    }

                    jobQueue.push(optionalJob.get(), true);

                    break;
                  }

                  int active = jobSetConcurrency.get(jobSet.getId()).incrementAndGet();

                  if (logJobsTrace()) {
                    LOGGER.trace(
                        MARKER.JOBS,
                        "Increased concurrency for job set to {} ({})",
                        active,
                        jobSet.getId());
                  }
                }

                run(processor, optionalJob.get());
              } else {
                break;
              }
            }
          }
        },
        0,
        1,
        TimeUnit.SECONDS);

    // check for orphaned jobs every minute
    polling.scheduleAtFixedRate(
        () -> {
          long oneMinuteAgo = Instant.now().minus(Duration.ofMinutes(1)).getEpochSecond();

          if (logJobsTrace()) {
            LOGGER.trace(MARKER.JOBS, "Checking for orphaned jobs (updatedAt < {})", oneMinuteAgo);
          }

          for (Job job : jobQueue.getTaken()) {
            // TODO: also update vector progress, remove raster check
            if (job.getType().equals("tile-seeding:raster:png")
                && job.getUpdatedAt().get() < oneMinuteAgo) {
              if (logJobsDebug()) {
                LOGGER.debug(MARKER.JOBS, "Found orphaned job, adding to queue again: {}", job);
              }
              jobQueue.push(job, true);
            }
          }

          if (logJobsTrace()) {
            LOGGER.trace(MARKER.JOBS, "Finished checking for orphaned jobs");
          }

          // remove done job sets older than one hour
          long oneHourAgo = Instant.now().minus(Duration.ofHours(1)).getEpochSecond();

          for (JobSet jobSet : jobQueue.getSets()) {
            if (jobSet.isDone() && jobSet.getUpdatedAt().get() < oneHourAgo) {
              jobQueue.doneSet(jobSet.getId());
            }
          }
        },
        1,
        1,
        TimeUnit.MINUTES);

    // log progress for active job sets every 5 seconds
    polling.scheduleAtFixedRate(
        () -> {
          if (logJobsDebug()) {
            jobSetConcurrency.forEach(
                (jobSetId, concurrency) -> {
                  JobSet jobSet = jobQueue.getSet(jobSetId);
                  if (Objects.nonNull(jobSet)) {
                    if (jobSet.getEntity().isPresent()) {
                      LogContext.put(LogContext.CONTEXT.SERVICE, jobSet.getEntity().get());
                    }
                    LOGGER.debug(
                        MARKER.JOBS,
                        "{} at {}%{}",
                        jobSet.getLabel(),
                        jobSet.getPercent(),
                        jobSet.getDescription().orElse(""));
                  }
                });
          }
        },
        5,
        5,
        TimeUnit.SECONDS);

    return AppLifeCycle.super.onStart(isStartupAsync);
  }

  private boolean shouldSuspend(JobSet jobSet, JobProcessor<?, ?> processor) {
    if (!jobSetConcurrencyMax.containsKey(jobSet.getId())) {
      jobSetConcurrencyMax.put(jobSet.getId(), processor.getConcurrency(jobSet));
      if (logJobsTrace()) {
        LOGGER.trace(
            MARKER.JOBS,
            "Max concurrency for job set is {} ({})",
            jobSetConcurrencyMax.get(jobSet.getId()),
            jobSet.getId());
      }
    }

    if (!jobSetConcurrency.containsKey(jobSet.getId())) {
      jobSetConcurrency.put(jobSet.getId(), new AtomicInteger(0));
    }

    return jobSetConcurrency.get(jobSet.getId()).get() >= jobSetConcurrencyMax.get(jobSet.getId());
  }

  private void run(JobProcessor<?, ?> processor, Job job) {
    executor.execute(
        () -> {
          Instant start = Instant.now();
          Optional<JobSet> jobSet = job.getPartOf().map(jobQueue::getSet);

          if (jobSet.isPresent() && jobSet.get().getEntity().isPresent()) {
            LogContext.put(LogContext.CONTEXT.SERVICE, jobSet.get().getEntity().get());
          }

          if (logJobsTrace()) {
            LOGGER.trace(MARKER.JOBS, "Processing job: {}", job);
          }

          JobResult result;
          try {
            result = processor.process(job, jobSet.orElse(null), jobQueue::push);
          } catch (Throwable e) {
            result = JobResult.error(e.getClass() + e.getMessage());
          }

          if (jobSet.isPresent()) {
            int active = jobSetConcurrency.get(jobSet.get().getId()).decrementAndGet();

            if (logJobsTrace()) {
              LOGGER.trace(
                  MARKER.JOBS,
                  "Decreased concurrency for job set to {} ({})",
                  active,
                  jobSet.get().getId());
            }
          }

          if (result.isSuccess()) {
            jobQueue.done(job.getId());
          } else if (result.isFailure()) {
            boolean retry = jobQueue.error(job.getId(), result.getError().get(), result.isRetry());
            if (!retry) {
              LOGGER.error("Error while processing job: {}", result.getError().get());
            }
          }

          if (jobSet.isPresent()) {
            if (jobSet.get().isDone()) {
              jobSetConcurrency.remove(jobSet.get().getId());
              jobSetConcurrencyMax.remove(jobSet.get().getId());
            }
          }

          if (logJobsTrace()) {
            if (result.isOnHold()) {
              LOGGER.trace(MARKER.JOBS, "Postponed job: {}", job);
            } else {
              long duration = Instant.now().toEpochMilli() - start.toEpochMilli();
              LOGGER.trace(MARKER.JOBS, "Processed job in {}: {}", pretty(duration), job);
            }
          }
        });
  }

  private static boolean logJobsTrace() {
    return LOGGER.isDebugEnabled(MARKER.JOBS) || LOGGER.isTraceEnabled();
  }

  private static boolean logJobsDebug() {
    return LOGGER.isDebugEnabled(MARKER.JOBS) || LOGGER.isDebugEnabled();
  }

  private static String pretty(long milliseconds) {
    Duration d = Duration.ofSeconds(milliseconds / 1000);
    return AmountFormats.wordBased(d, Locale.ENGLISH);
  }
}
