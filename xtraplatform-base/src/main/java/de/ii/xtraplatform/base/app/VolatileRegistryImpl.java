/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.base.domain.resiliency.Volatile2;
import de.ii.xtraplatform.base.domain.resiliency.Volatile2.Polling;
import de.ii.xtraplatform.base.domain.resiliency.Volatile2.State;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class VolatileRegistryImpl implements VolatileRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(VolatileRegistryImpl.class);

  private final ScheduledExecutorService executorService;
  private final Map<String, Volatile2> volatiles;
  private final Map<String, List<ChangeHandler>> watchers;
  private final Map<String, Polling> polls;
  private final Map<String, Integer> intervals;
  private final Queue<Runnable> cancelTasks;
  private final List<BiConsumer<String, Volatile2>> onRegister;
  private final List<Consumer<String>> onUnRegister;
  private ScheduledFuture<?> currentSchedule;
  private int currentRate;

  @Inject
  VolatileRegistryImpl() {
    this.executorService =
        MoreExecutors.getExitingScheduledExecutorService(
            (ScheduledThreadPoolExecutor)
                Executors.newScheduledThreadPool(
                    2, new ThreadFactoryBuilder().setNameFormat("volatile.polling-%d").build()));
    this.volatiles = new ConcurrentHashMap<>();
    this.watchers = new ConcurrentHashMap<>();
    this.polls = new ConcurrentHashMap<>();
    this.intervals = new ConcurrentHashMap<>();
    this.cancelTasks = new ArrayDeque<>();
    this.onRegister = new ArrayList<>();
    this.onUnRegister = new ArrayList<>();
    this.currentSchedule = null;
    this.currentRate = 0;
  }

  @Override
  public void register(Volatile2 dependency) {
    synchronized (this) {
      if (volatiles.containsKey(dependency.getUniqueKey())) {
        return;
      }

      volatiles.put(dependency.getUniqueKey(), dependency);
      watchers.put(dependency.getUniqueKey(), new ArrayList<>());

      onRegister.forEach(listener -> listener.accept(dependency.getUniqueKey(), dependency));

      LOGGER.debug(
          "Volatile registered: {} {}",
          dependency.getUniqueKey(),
          dependency instanceof Polling ? "(polling)" : "");

      if (dependency instanceof Polling) {
        Polling polling = (Polling) dependency;
        if (polling.getIntervalMs() > 0) {
          polling.poll();

          polls.put(dependency.getUniqueKey(), polling);
          intervals.put(dependency.getUniqueKey(), 0);

          // TODO
          schedulePoll((polling).getIntervalMs());
        }
      }
    }
  }

  @Override
  public synchronized void unregister(Volatile2 dependency) {
    synchronized (this) {
      volatiles.remove(dependency.getUniqueKey());
      watchers.remove(dependency.getUniqueKey());

      onUnRegister.forEach(listener -> listener.accept(dependency.getUniqueKey()));

      if (dependency instanceof Polling) {
        polls.remove(dependency.getUniqueKey());
      }
    }
  }

  @Override
  public void change(Volatile2 dependency, State from, State to) {
    String key = dependency.getUniqueKey();

    if (LOGGER.isDebugEnabled(MARKER.DI)) {
      LOGGER.debug("Volatile state changed from {} to {}: {}", from, to, key);
    }

    synchronized (watchers) {
      if (watchers.containsKey(key)) {
        for (ChangeHandler handler : watchers.get(key)) {
          if (Objects.nonNull(handler)) {
            try {
              handler.change(from, to);
            } catch (Throwable e) {
              // ignore
              if (LOGGER.isDebugEnabled()) {
                LogContext.errorAsDebug(LOGGER, e, "Error in volatile watcher");
              }
            }
          }
        }
      }
    }
  }

  @Override
  public Runnable watch(Volatile2 dependency, ChangeHandler handler) {
    String key = dependency.getUniqueKey();

    synchronized (watchers) {
      if (watchers.containsKey(key)) {
        watchers.get(key).add(handler);
        int index = watchers.get(key).size() - 1;

        return () -> {
          if (watchers.containsKey(key)) {
            watchers.get(key).set(index, null);
          }
        };
      }
    }
    return () -> {};
  }

  @Override
  public CompletionStage<Void> onAvailable(Volatile2... volatiles) {
    CompletableFuture<Void> onAvailable = new CompletableFuture<>();
    Map<String, State> states = new ConcurrentHashMap<>();
    List<Runnable> unwatchs = new ArrayList<>();

    synchronized (this) {
      for (Volatile2 vol : volatiles) {
        states.put(vol.getUniqueKey(), vol.getState());
      }

      if (states.values().stream().allMatch(state -> state == State.AVAILABLE)) {
        onAvailable.complete(null);
        return onAvailable;
      }

      for (Volatile2 vol : volatiles) {
        unwatchs.add(
            watch(
                vol,
                (from, to) -> {
                  states.put(vol.getUniqueKey(), to);
                  if (states.values().stream().allMatch(state -> state == State.AVAILABLE)) {
                    onAvailable.complete(null);
                    unwatchs.forEach(Runnable::run);
                  }
                }));
      }
    }
    return onAvailable;
  }

  @Override
  public void listen(BiConsumer<String, Volatile2> onRegister, Consumer<String> onUnRegister) {
    synchronized (this) {
      this.onRegister.add(onRegister);
      this.onUnRegister.add(onUnRegister);

      volatiles.forEach(onRegister);
    }
  }

  private void schedulePoll(int delayMs) {
    if (delayMs > 0 && currentRate > delayMs || currentRate <= 0) {
      if (Objects.nonNull(currentSchedule)) {
        // LOGGER.debug("Cancelling current polling schedule");
        currentSchedule.cancel(false);
      }

      this.currentRate = delayMs;
      if (LOGGER.isDebugEnabled(MARKER.DI)) {
        LOGGER.debug("Scheduling polling every {}ms", delayMs);
      }
      this.currentSchedule =
          executorService.scheduleWithFixedDelay(
              () -> {
                while (!cancelTasks.isEmpty()) {
                  cancelTasks.remove().run();
                }

                for (Map.Entry<String, Integer> entry : intervals.entrySet()) {
                  String key = entry.getKey();
                  int interval = entry.getValue() - delayMs;

                  if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Checking {} in {}ms", key, interval);
                  }

                  if (interval <= 0) {
                    if (LOGGER.isTraceEnabled()) {
                      LOGGER.trace("Checking {} now", key);
                    }
                    Polling polling = polls.get(key);
                    interval = polling.getIntervalMs();

                    Future<?> future = executorService.submit(polling::poll);
                    cancelTasks.add(() -> future.cancel(true));

                    // Runnable cancelTask = () -> future.cancel(true);
                    // executorService.schedule(cancelTask, 1000, TimeUnit.MILLISECONDS);

                    // polling.poll();
                  }

                  intervals.put(key, interval);
                }
              },
              0,
              delayMs,
              TimeUnit.MILLISECONDS);
    }
  }
}
