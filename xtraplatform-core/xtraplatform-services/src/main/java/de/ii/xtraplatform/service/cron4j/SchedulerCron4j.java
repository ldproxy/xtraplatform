/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.scheduler.cron4j;

import de.ii.xtraplatform.scheduler.api.Scheduler;
import de.ii.xtraplatform.scheduler.api.SchedulerTask;
import de.ii.xtraplatform.scheduler.api.Task;
import de.ii.xtraplatform.scheduler.api.TaskQueue;
import de.ii.xtraplatform.scheduler.api.TaskStatus;
import it.sauronsoftware.cron4j.TaskExecutor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.xtraplatform.scheduler.api.SchedulerTask)",
        onArrival = "onTaskArrival",
        onDeparture = "onTaskDeparture")
public class SchedulerCron4j implements Scheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerCron4j.class);

    @Context
    private BundleContext context;

    private final it.sauronsoftware.cron4j.Scheduler scheduler;

    public SchedulerCron4j() {
        this.scheduler = new it.sauronsoftware.cron4j.Scheduler();
        scheduler.setDaemon(true);
    }

    @Validate
    public void start() {
        scheduler.start();
    }

    @Invalidate
    public void stop() {
        scheduler.stop();
    }

    public synchronized void onTaskArrival(ServiceReference<SchedulerTask> ref) {
        SchedulerTask task = context.getService(ref);

        String id = scheduler.schedule(task.getPattern(), task.getTask());
        task.setId(id);
    }

    public synchronized void onTaskDeparture(ServiceReference<SchedulerTask> ref) {
        SchedulerTask task = context.getService(ref);

        scheduler.deschedule(task.getId());
    }

    @Override
    public TaskStatus launch(Task task) {

        final TaskExecutor taskExecutor = scheduler.launch(new TaskCron4j(task));

        return new TaskStatusCron4j(task.getId(), task.getLabel(), taskExecutor);
    }

    @Override
    public TaskQueue createQueue(String id) {
        return new TaskQueue() {
            private final BlockingQueue<Pair<Task, CompletableFuture<TaskStatus>>> queue = new LinkedBlockingQueue<>();
            private TaskStatus currentTask;

            @Override
            public synchronized CompletableFuture<TaskStatus> launch(Task task) {
                LOGGER.debug("Queuing task {}", task.getLabel());
                final CompletableFuture<TaskStatus> taskStatusCompletableFuture = new CompletableFuture<>();

                queue.offer(new ImmutablePair<>(task, taskStatusCompletableFuture));

                checkQueue(false);

                return taskStatusCompletableFuture;
            }

            @Override
            public List<Task> getFutureTasks() {
                return queue.stream().map(Pair::getLeft).collect(Collectors.toList());
            }

            @Override
            public Optional<TaskStatus> getCurrentTask() {
                return Optional.ofNullable(currentTask);
            }

            private synchronized void checkQueue(boolean currentIsDone) {
                if (currentIsDone) {
                    this.currentTask = null;
                }
                if (Objects.isNull(currentTask) || currentTask.isDone()) {
                    final Pair<Task, CompletableFuture<TaskStatus>> task = queue.poll();
                    if (Objects.nonNull(task)) {
                        LOGGER.debug("Launching task {}", task.getLeft().getLabel());
                        this.currentTask = SchedulerCron4j.this.launch(task.getLeft());
                        task.getRight().complete(currentTask);
                        currentTask.onDone(() -> {
                            LOGGER.debug("Finalized task {}", task.getLeft().getLabel());
                            checkQueue(true);
                        });
                    }
                }
            }
        };
    }
}
