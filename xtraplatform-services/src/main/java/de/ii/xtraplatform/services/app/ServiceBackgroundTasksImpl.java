/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import dagger.Lazy;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.entities.domain.EntityRegistry;
import de.ii.xtraplatform.entities.domain.Reloadable;
import de.ii.xtraplatform.services.domain.Scheduler;
import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.services.domain.ServiceBackgroundTask;
import de.ii.xtraplatform.services.domain.ServiceBackgroundTasks;
import de.ii.xtraplatform.services.domain.Task;
import de.ii.xtraplatform.services.domain.TaskQueue;
import de.ii.xtraplatform.services.domain.TaskStatus;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class ServiceBackgroundTasksImpl implements ServiceBackgroundTasks, AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceBackgroundTasksImpl.class);

  private final Lazy<Set<ServiceBackgroundTask<?>>> tasks;
  private final Scheduler scheduler;
  private final TaskQueue commonQueue;
  private final Map<String, TaskQueue> taskQueues;
  private final Map<String, List<String>> cronJobs;

  @Inject
  ServiceBackgroundTasksImpl(
      AppContext appContext,
      Scheduler scheduler,
      EntityRegistry entityRegistry,
      Lazy<Set<ServiceBackgroundTask<?>>> tasks) {
    this.tasks = tasks;
    this.scheduler = scheduler;
    this.commonQueue =
        scheduler.createQueue(
            ServiceBackgroundTasks.COMMON_QUEUE, appContext.getConfiguration().getJobConcurrency());
    this.taskQueues = new ConcurrentHashMap<>();
    this.cronJobs = new ConcurrentHashMap<>();
    taskQueues.put(COMMON_QUEUE, commonQueue);
    entityRegistry.addEntityListener(Service.class, this::onServiceStart, true);
    entityRegistry.addEntityGoneListener(Service.class, this::onServiceStop);
  }

  @Override
  public int getPriority() {
    // stop first
    return 3000;
  }

  @Override
  public CompletionStage<Void> onStart(boolean isStartupAsync) {
    tasks.get().forEach(task -> task.setTrigger(this::onServiceReload));

    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void onStop() {
    commonQueue.getFutureTasks().forEach(commonQueue::remove);
    commonQueue.getCurrentTask().ifPresent(TaskStatus::stop);
  }

  private <T extends Service> void onServiceStart(T service) {
    if (service instanceof Reloadable) {
      ((Reloadable) service).addReloadListener(service.getClass(), this::onServiceReload);
    }
    startTasks(service);
  }

  private <T extends Service> void onServiceReload(T service) {
    stopTasks(service);
    startTasks(service);
  }

  private <T extends Service> void onServiceStop(T service) {
    stopTasks(service);
  }

  private <T extends Service> void startTasks(T service) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Starting background tasks for service '{}'", service.getId());
    }
    tasks
        .get()
        .forEach(
            task -> {
              scheduleIfMatching(service, task);
            });
  }

  private <T extends Service> void stopTasks(T service) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Stopping background tasks for service '{}'", service.getId());
    }
    getCurrentTaskForService(service.getId()).ifPresent(TaskStatus::stop);
    commonQueue.getFutureTasks().stream()
        .filter(task -> Objects.equals(task.getId(), service.getId()))
        .forEach(commonQueue::remove);
    stopCronJobs(service.getId());
  }

  private <T extends Service, U extends Service> void scheduleIfMatching(
      T service, ServiceBackgroundTask<U> task) {
    Class<U> serviceType = task.getServiceType();
    if (serviceType.isAssignableFrom(service.getClass())) {
      schedule(serviceType.cast(service), task);
    }
  }

  private <T extends Service> void schedule(T service, ServiceBackgroundTask<T> task) {
    if (task.runOnStart(service)) {
      taskQueues.get(task.getQueue()).launch(task.getTask(service, task.getLabel()), 5000);
    }
    if (task.runPeriodic(service).isPresent()) {
      TaskQueue taskQueue = taskQueues.get(task.getQueue());
      Task task1 = task.getTask(service, task.getLabel());
      startCronJob(service.getId(), task.runPeriodic(service).get(), () -> taskQueue.launch(task1));
    }
  }

  private void startCronJob(String serviceId, String cronPattern, Runnable runnable) {
    if (!cronJobs.containsKey(serviceId)) {
      cronJobs.put(serviceId, new CopyOnWriteArrayList<>());
    }
    String jobId = scheduler.schedule(runnable, cronPattern);
    cronJobs.get(serviceId).add(jobId);
  }

  private void stopCronJobs(String serviceId) {
    if (cronJobs.containsKey(serviceId)) {
      cronJobs.get(serviceId).forEach(scheduler::deschedule);
    }
  }

  @Override
  public TaskQueue createQueue(String taskType) {
    TaskQueue queue = scheduler.createQueue(taskType, 1);
    taskQueues.put(taskType, queue);

    return queue;
  }

  @Override
  public Optional<TaskStatus> getCurrentTask() {
    return commonQueue.getCurrentTask();
  }

  @Override
  public Optional<TaskStatus> getCurrentTaskForService(String id) {
    return getCurrentTask().filter(taskStatus -> Objects.equals(taskStatus.getId(), id));
  }
}
