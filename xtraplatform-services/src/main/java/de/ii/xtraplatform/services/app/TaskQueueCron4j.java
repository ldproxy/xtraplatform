/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.app;

import de.ii.xtraplatform.services.domain.Task;
import de.ii.xtraplatform.services.domain.TaskQueue;
import de.ii.xtraplatform.services.domain.TaskStatus;
import it.sauronsoftware.cron4j.TaskExecutor;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Cron4j implementation of TaskQueue with support for parallel execution and task queuing. */
@SuppressWarnings("PMD.TooManyMethods")
public class TaskQueueCron4j implements TaskQueue {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskQueueCron4j.class);

  private final it.sauronsoftware.cron4j.Scheduler scheduler;
  private final BiConsumer<TaskCron4j, TaskStatus> loggingHandler;
  private final BlockingQueue<Pair<Task, CompletableFuture<TaskStatus>>> queue;
  private final BlockingQueue<TaskStatus> currentTasks;
  private final BlockingQueue<Integer> threadNumbers;

  public TaskQueueCron4j(
      it.sauronsoftware.cron4j.Scheduler scheduler,
      BiConsumer<TaskCron4j, TaskStatus> loggingHandler,
      int maxConcurrentTasks) {
    this.scheduler = scheduler;
    this.loggingHandler = loggingHandler;
    this.queue = new LinkedBlockingQueue<>();
    this.currentTasks = new LinkedBlockingQueue<>(maxConcurrentTasks);
    this.threadNumbers =
        new LinkedBlockingQueue<>(
            IntStream.rangeClosed(1, maxConcurrentTasks).boxed().collect(Collectors.toList()));
  }

  @Override
  public CompletableFuture<TaskStatus> launch(Task task) {
    synchronized (this) {
      Thread.currentThread().setName("bg-task-0");
      task.logContext();
      cleanup();

      if (isTaskAlreadyRunning(task)) {
        logTaskIgnored(task, "already running");
        return CompletableFuture.failedFuture(new IllegalArgumentException());
      }

      if (isTaskAlreadyQueued(task)) {
        logTaskIgnored(task, "already in queue");
        return CompletableFuture.failedFuture(new IllegalArgumentException());
      }

      return queueTask(task);
    }
  }

  @Override
  public CompletableFuture<TaskStatus> launch(Task task, long delay) {
    CompletableFuture<TaskStatus> completableFuture = new CompletableFuture<>();
    ForkJoinPool.commonPool()
        .execute(
            () -> {
              try {
                Thread.sleep(delay);
              } catch (InterruptedException e) {
                // ignore
              }
              launch(task).thenAccept(completableFuture::complete);
            });
    return completableFuture;
  }

  @Override
  public void remove(Task task) {
    List<Pair<Task, CompletableFuture<TaskStatus>>> toRemove =
        queue.stream()
            .filter(entry -> Objects.equals(task, entry.getLeft()))
            .collect(Collectors.toList());

    toRemove.forEach(
        entry -> {
          boolean removed = queue.remove(entry);
          if (removed && LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                "REMOVED TASK {} -> {}", entry.getLeft().getLabel(), entry.getLeft().getId());
          }
        });
  }

  @Override
  public List<Task> getFutureTasks() {
    return queue.stream().map(Pair::getLeft).collect(Collectors.toList());
  }

  @Override
  public Optional<TaskStatus> getCurrentTask() {
    return Optional.ofNullable(currentTasks.peek());
  }

  private boolean isTaskAlreadyRunning(Task task) {
    return currentTasks.stream()
        .filter(Objects::nonNull)
        .anyMatch(
            runningTask ->
                Objects.equals(runningTask.getId(), task.getId())
                    && Objects.equals(runningTask.getLabel(), task.getLabel()));
  }

  private boolean isTaskAlreadyQueued(Task task) {
    return getFutureTasks().stream()
        .anyMatch(
            futureTask ->
                Objects.equals(futureTask.getId(), task.getId())
                    && Objects.equals(futureTask.getLabel(), task.getLabel()));
  }

  private void logTaskIgnored(Task task, String reason) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Ignoring task '{}' for '{}', {}", task.getLabel(), task.getId(), reason);
    }
  }

  private CompletableFuture<TaskStatus> queueTask(Task task) {
    final CompletableFuture<TaskStatus> taskStatusCompletableFuture = new CompletableFuture<>();
    queue.offer(new ImmutablePair<>(task, taskStatusCompletableFuture));
    checkQueue();
    return taskStatusCompletableFuture;
  }

  private void checkQueue() {
    synchronized (this) {
      cleanup();

      if (currentTasks.remainingCapacity() <= 0) {
        return;
      }

      final Pair<Task, CompletableFuture<TaskStatus>> taskPair = queue.poll();
      if (taskPair == null) {
        return;
      }

      Task task = taskPair.getLeft();
      if (shouldLaunchAsPartialTasks(task)) {
        launchPartialTasks(task);
      } else {
        launchSingleTask(task, taskPair.getRight());
      }
    }
  }

  private boolean shouldLaunchAsPartialTasks(Task task) {
    return task.getMaxPartials() > 1 && currentTasks.remainingCapacity() > 1;
  }

  private void launchPartialTasks(Task task) {
    int maxPartials = Math.min(currentTasks.remainingCapacity(), task.getMaxPartials());
    AtomicInteger activePartials = new AtomicInteger(maxPartials);

    for (int i = 1; i <= maxPartials; i++) {
      int threadNumber = Objects.requireNonNullElse(threadNumbers.poll(), 1);
      @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
      TaskCron4j taskCron4j = new TaskCron4j(task, maxPartials, i, threadNumber, activePartials);
      TaskStatus currentTask = createAndLaunchTask(taskCron4j);

      setupPartialTaskCompletion(currentTask, threadNumber, activePartials);
      currentTasks.offer(currentTask);
    }
  }

  private void launchSingleTask(Task task, CompletableFuture<TaskStatus> future) {
    int threadNumber = Objects.requireNonNullElse(threadNumbers.poll(), 1);
    TaskCron4j taskCron4j = new TaskCron4j(task, threadNumber);
    TaskStatus currentTask = createAndLaunchTask(taskCron4j);

    setupSingleTaskCompletion(currentTask, threadNumber);
    currentTasks.offer(currentTask);
    future.complete(currentTask);
  }

  private TaskStatus createAndLaunchTask(TaskCron4j taskCron4j) {
    final TaskExecutor taskExecutor = scheduler.launch(taskCron4j);
    TaskStatus currentTask = new TaskStatusCron4j(taskCron4j, taskExecutor);
    loggingHandler.accept(taskCron4j, currentTask);
    return currentTask;
  }

  private void setupPartialTaskCompletion(
      TaskStatus currentTask, int threadNumber, AtomicInteger activePartials) {
    currentTask.onDone(
        throwable -> {
          activePartials.decrementAndGet();
          threadNumbers.offer(threadNumber);
          checkQueue();
        });
  }

  private void setupSingleTaskCompletion(TaskStatus currentTask, int threadNumber) {
    currentTask.onDone(
        throwable -> {
          threadNumbers.offer(threadNumber);
          checkQueue();
        });
  }

  private void cleanup() {
    synchronized (this) {
      currentTasks.removeIf(TaskStatus::isDone);
    }
  }
}
