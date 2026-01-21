/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.streams.app;

import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.util.Triple;
import de.ii.xtraplatform.streams.app.ReactiveRx.SubscriberRx;
import de.ii.xtraplatform.streams.domain.Reactive.Runner;
import de.ii.xtraplatform.streams.domain.Reactive.Stream;
import de.ii.xtraplatform.streams.domain.Reactive.StreamContext;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

// NOTE: the queue was introduced as a mean to protect the connection pool and prevent deadlocks
// because of a bug (running.get() < queueSize instead of running.get() < capacity) it was never
// used
// despite that there were no problems with deadlocks and enabling it slightly decreases performance
// so I guess the options are to either remove it or enable it and use DYNAMIC_CAPACITY for
// FeatureStreams
public class RunnerRx implements Runner {

  private final Scheduler scheduler;
  private final int capacity;
  private final int queueSize;
  private final Queue<Runnable> queue;
  private final AtomicInteger running;

  public RunnerRx() {
    this(Runner.DYNAMIC_CAPACITY, Runner.DYNAMIC_CAPACITY);
  }

  public RunnerRx(int capacity, int queueSize) {
    this(getConfig(capacity), capacity, queueSize);
  }

  RunnerRx(ExecutorService executorService, int capacity, int queueSize) {
    if (capacity == 0) {
      throw new IllegalArgumentException("invalid capacity: 0");
    }

    // NOPMD - TODO: thread names
    this.scheduler = Schedulers.from(executorService);
    scheduler.start();

    this.capacity = capacity;
    this.queueSize = queueSize;
    this.queue = new LinkedList<>();
    this.running = new AtomicInteger(0);
  }

  @Override
  public <X> CompletionStage<X> run(Stream<X> stream) {
    return runGraph(ReactiveRx.getGraph(stream));
  }

  private <T, U> CompletionStage<U> runGraph(
      Triple<Flowable<T>, SubscriberRx<T>, StreamContext<U>> graph) {
    CompletableFuture<U> result = new CompletableFuture<>();
    Flowable<T> flowable = graph.first();
    SubscriberRx<T> subscriber = graph.second();
    StreamContext<U> context = graph.third();

    if (getCapacity() == Runner.DYNAMIC_CAPACITY) {
      flowable
          .subscribeOn(scheduler)
          .doOnError(throwable -> context.onError(result, throwable))
          .doOnComplete(() -> context.onComplete(result))
          .subscribe(subscriber.onError(throwable -> context.onError(result, throwable)));
    } else {
      Runnable task =
          () -> {
            flowable
                .subscribeOn(scheduler)
                .doOnError(
                    throwable -> {
                      context.onError(result, throwable);

                      runNext();
                    })
                .doOnComplete(
                    () ->
                        LogContext.withMdc(
                                () -> {
                                  context.onComplete(result);

                                  runNext();
                                })
                            .run())
                .subscribe(subscriber.onError(throwable -> context.onError(result, throwable)));
          };
      run(task);
    }

    return result;
  }

  private void run(Runnable task) {
    synchronized (running) {
      if (running.get() < queueSize) { // correct would be running.get() < capacity, see above
        running.incrementAndGet();
        task.run();
      } else {
        /*if (queue.size() > queueSize) {
          queue.poll();
        }*/
        queue.offer(task);
      }
    }
  }

  private void runNext() {
    synchronized (running) {
      int current = running.get() - 1;
      if (current < queueSize) { // correct would be current < capacity, see above
        Runnable task = queue.poll();
        if (Objects.nonNull(task)) {
          task.run();
        } else {
          running.decrementAndGet();
        }
      }
    }
  }

  @Override
  public int getCapacity() {
    return capacity;
  }

  @Override
  public int getActiveStreams() {
    return running.get();
  }

  private static ExecutorService getConfig(int capacity) {
    return capacity == Runner.DYNAMIC_CAPACITY
        ? getDefaultConfig()
        : createExecutorService(capacity);
  }

  private static ExecutorService getDefaultConfig() {
    return createExecutorService(64);
  }

  private static ExecutorService createExecutorService(int parallelismMax) {

    return Executors.newWorkStealingPool(Math.max(1, parallelismMax));
  }

  @Override
  public void close() {
    if (Objects.nonNull(scheduler)) {
      scheduler.shutdown();
    }
  }
}
