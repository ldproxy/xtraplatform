/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableJobSet.Builder.class)
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public interface JobSet extends BaseJob {

  @SuppressWarnings("PMD.DataClass")
  final class JobSetConfiguration {
    private final String type;
    private final int priority;
    private final String entity;
    private final String label;
    private final String description;
    private final JobSetDetails details;

    private JobSetConfiguration(
        String type,
        int priority,
        String entity,
        String label,
        String description,
        JobSetDetails details) {
      this.type = type;
      this.priority = priority;
      this.entity = entity;
      this.label = label;
      this.description = description;
      this.details = details;
    }

    public static JobSetConfiguration of(
        String type,
        int priority,
        String entity,
        String label,
        String description,
        JobSetDetails details) {
      return new JobSetConfiguration(type, priority, entity, label, description, details);
    }

    public String getType() {
      return type;
    }

    public int getPriority() {
      return priority;
    }

    public String getEntity() {
      return entity;
    }

    public String getLabel() {
      return label;
    }

    public String getDescription() {
      return description;
    }

    public JobSetDetails getDetails() {
      return details;
    }
  }

  interface JobSetDetails {

    void init(Map<String, Object> parameters);

    Map<String, Object> initJson(Map<String, Object> params);

    void update(Map<String, Object> parameters);

    Map<String, Object> updateJson(Map<String, Object> detailParameters);

    void reset(Job job);

    String getLabel();
  }

  @Override
  List<JobSet> getFollowUps();

  static JobSet of(JobSetConfiguration config) {
    return new ImmutableJobSet.Builder()
        .type(config.getType())
        .priority(config.getPriority())
        .entity(config.getEntity())
        .label(config.getLabel())
        .description(config.getDescription())
        .details(config.getDetails())
        .startedAt(new AtomicLong())
        .total(new AtomicInteger(0))
        .current(new AtomicInteger(0))
        .build();
  }

  static JobSet of(
      String type,
      int priority,
      String entity,
      String label,
      String description,
      JobSetDetails details) {
    return of(JobSetConfiguration.of(type, priority, entity, label, description, details));
  }

  default JobSet with(Job setup, Job cleanup) {
    return new ImmutableJobSet.Builder()
        .from(this)
        .setup(setup.with(this.getId()))
        .cleanup(cleanup.with(this.getId()))
        .build();
  }

  default JobSet with(JobSet... followUps) {
    return new ImmutableJobSet.Builder().from(this).addFollowUps(followUps).build();
  }

  default JobSet with(String description, JobSetDetails details) {
    return new ImmutableJobSet.Builder()
        .from(this)
        .description(description)
        .details(details)
        .build();
  }

  default JobSet with(JobSetDetails details) {
    return new ImmutableJobSet.Builder().from(this).details(details).build();
  }

  default void start() {
    getStartedAt().set(Instant.now().getEpochSecond());
  }

  default List<? extends BaseJob> done(Job job) {
    if (getSetup().isPresent() && Objects.equals(job.getId(), getSetup().get().getId())) {
      return List.of();
    }

    if (getCleanup().isPresent() && Objects.equals(job.getId(), getCleanup().get().getId())) {
      return getFollowUps();
    }

    getUpdatedAt().set(Instant.now().getEpochSecond());

    List<String> jobErrors = job.getErrors().get();
    if (!jobErrors.isEmpty()) {
      getErrors()
          .getAndUpdate(
              existingErrors -> {
                List<String> combined = new java.util.ArrayList<>(existingErrors);
                combined.addAll(jobErrors);
                return ImmutableList.copyOf(combined);
              });
    }

    if (isDone() && getFinishedAt().get() == -1) {
      getFinishedAt().set(Instant.now().getEpochSecond());
      return getCleanup().isPresent() ? List.of(getCleanup().get()) : getFollowUps();
    }

    return List.of();
  }

  @Value.Default
  default String getLabel() {
    return getType();
  }

  Optional<String> getDescription();

  Optional<String> getEntity();

  Optional<Job> getSetup();

  Optional<Job> getCleanup();
}
