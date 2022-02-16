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
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunnerRx implements Runner {

  private static final Logger LOGGER = LoggerFactory.getLogger(RunnerRx.class);

  private final Scheduler scheduler;
  private final ExecutorService executorService;
  private final String name;
  private final int capacity;
  private final int queueSize;
  private final ConcurrentLinkedQueue<Runnable> queue;
  private final AtomicInteger running;

  public RunnerRx(String name) {
    this(name, Runner.DYNAMIC_CAPACITY, Runner.DYNAMIC_CAPACITY);
  }

  public RunnerRx(String name, int capacity, int queueSize) {
    this(getConfig(name, capacity), name, capacity, queueSize);
  }

  RunnerRx(ExecutorService executorService, String name, int capacity, int queueSize) {
    if (capacity != 0) {
      // TODO
      getDispatcherName(name);
      this.executorService = executorService;
      this.scheduler = Schedulers.from(executorService);
      scheduler.start();
    } else {
      this.executorService = null;
      this.scheduler = null;
    }
    this.name = name;
    this.capacity = capacity;
    this.queueSize = queueSize;
    this.queue = new ConcurrentLinkedQueue<>();
    this.running = new AtomicInteger(0);
  }

  // 2x
  /*@Override
  @Deprecated
  public <T, U, V> CompletionStage<V> run(Source<T, U> source, Sink<T, CompletionStage<V>> sink) {
    //return run(LogContextStream.graphWithMdc(source, sink, Keep.right()));
    throw new UnsupportedOperationException();
  }

  // 5x
  @Override
  @Deprecated
  public <U> CompletionStage<U> run(RunnableGraphWrapper<U> graph) {
    //return runGraph(graph.getGraph());
    throw new UnsupportedOperationException();
  }*/

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
                            }).run())
                .subscribe(subscriber.onError(throwable -> context.onError(result, throwable)));
          };
      run(task);
    }

    return result;
  }

  private void run(Runnable task) {
    synchronized (running) {
      if (running.get() < queueSize) {
        running.incrementAndGet();
        task.run();
      } else {
        queue.offer(task);
      }
    }
  }

  private void runNext() {
    synchronized (running) {
      int current = running.get() - 1;
      if (current < queueSize) {
        Runnable task = queue.poll();
        if (Objects.nonNull(task)) {
          task.run();
        } else {
          running.decrementAndGet();
        }
      }
    }
  }

  /*@Override
  public ExecutionContextExecutor getDispatcher() {
    return (ExecutionContextExecutor) executorService;
  }*/

  @Override
  public int getCapacity() {
    return capacity;
  }

  private static ExecutorService getConfig(String name, int capacity) {
    return capacity == Runner.DYNAMIC_CAPACITY
        ? getDefaultConfig(name)
        : getConfig(name, capacity, capacity);
  }

  private static ExecutorService getDefaultConfig(String name) {
    return getConfig(name, 8, 64);
  }

  // TODO
  private static ExecutorService getConfig(String name, int parallelismMin, int parallelismMax) {
    return Executors.newWorkStealingPool(parallelismMax);
    /*return ConfigFactory.parseMap(
    new ImmutableMap.Builder<String, Object>()
        .put("akka.stdout-loglevel", "OFF")
        .put("akka.loglevel", "INFO")
        .put("akka.loggers", ImmutableList.of("akka.event.slf4j.Slf4jLogger"))
        .put("akka.logging-filter", "akka.event.slf4j.Slf4jLoggingFilter")
        // .put("akka.log-config-on-start", true)
        .put(String.format("%s.type", getDispatcherName(name)), "Dispatcher")
        // .put(String.format("%s.executor", getDispatcherName(name)), "fork-join-executor")
        .put(
            String.format("%s.executor", getDispatcherName(name)),
            "de.ii.xtraplatform.streams.app.StreamExecutorServiceConfigurator")
        .put(
            String.format("%s.fork-join-executor.parallelism-min", getDispatcherName(name)),
            parallelismMin)
        .put(
            String.format("%s.fork-join-executor.parallelism-factor", getDispatcherName(name)),
            1.0)
        .put(
            String.format("%s.fork-join-executor.parallelism-max", getDispatcherName(name)),
            parallelismMax)
        .put(
            String.format("%s.fork-join-executor.task-peeking-mode", getDispatcherName(name)),
            "FIFO")
        .build());*/
  }

  private static String getDispatcherName(String name) {
    return String.format("stream.%s", name);
  }

  @Override
  public void close() {
    if (Objects.nonNull(scheduler)) {
      scheduler.shutdown();
    }
  }
}
