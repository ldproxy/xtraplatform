/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.jobs.domain.Job;
import de.ii.xtraplatform.jobs.domain.JobQueue;
import de.ii.xtraplatform.ops.domain.OpsEndpoint;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class OpsEndpointJobs implements OpsEndpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpsEndpointJobs.class);

  private final JobQueue jobQueue;
  private final ObjectMapper objectMapper;

  @Inject
  public OpsEndpointJobs(AppContext appContext, Jackson jackson, JobQueue jobQueue) {
    // TODO: if enabled, start embedded queue
    this.objectMapper = jackson.getDefaultObjectMapper();
    this.jobQueue = jobQueue;
  }

  @Override
  public String getEntrypoint() {
    return "jobs";
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getJobs() throws JsonProcessingException {
    Map<String, Object> jobs =
        Map.of(
            "sets",
            jobQueue.getSets(),
            "open",
            jobQueue.getOpen(),
            "taken",
            jobQueue.getTaken(),
            "failed",
            jobQueue.getFailed());

    try {
      String s = objectMapper.writeValueAsString(jobs);
      return Response.ok(s).build();
    } catch (Throwable e) {
      LOGGER.error("Error while serializing jobs", e);
      throw e;
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public synchronized Response takeJob(Map<String, String> executor)
      throws JsonProcessingException {
    Optional<Job> job = jobQueue.take(executor.get("type"), executor.get("id"));

    if (job.isPresent()) {
      if (LOGGER.isTraceEnabled() || LOGGER.isTraceEnabled(MARKER.JOBS)) {
        LOGGER.trace(
            MARKER.JOBS,
            "Job {} taken by remote executor {}",
            job.get().getId(),
            executor.get("id"));
      }
      return Response.ok(objectMapper.writeValueAsString(job.get())).build();
    }

    return Response.noContent().build();
  }

  @POST
  @Path("/{jobId}")
  @Consumes(MediaType.APPLICATION_JSON)
  public synchronized Response updateJob(
      @PathParam("jobId") String jobId, Map<String, String> progress)
      throws JsonProcessingException {
    Optional<Job> job =
        jobQueue.getTaken().stream()
            .filter(job1 -> Objects.equals(job1.getId(), jobId))
            .findFirst();

    if (job.isPresent()) {
      job.get().getUpdatedAt().set(Instant.now().getEpochSecond());
      if (progress.containsKey("current")) {
        job.get().getCurrent().set(Integer.parseInt(progress.get("current")));
      }
    }

    return Response.noContent().build();
  }

  @DELETE
  @Path("/{jobId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public synchronized Response closeJob(
      @PathParam("jobId") String jobId, Map<String, String> result) throws JsonProcessingException {
    if (result.containsKey("error") && Objects.nonNull(result.get("error"))) {
      boolean retry =
          jobQueue.error(jobId, result.get("error"), Boolean.parseBoolean(result.get("retry")));

      if (LOGGER.isTraceEnabled() || LOGGER.isTraceEnabled(MARKER.JOBS)) {
        LOGGER.trace(
            MARKER.JOBS, "Job {} marked as error by remote executor (retry: {})", jobId, retry);
      }

      return Response.noContent().build();
    }

    if (jobQueue.done(jobId)) {
      if (LOGGER.isTraceEnabled() || LOGGER.isTraceEnabled(MARKER.JOBS)) {
        LOGGER.trace(MARKER.JOBS, "Job {} marked as done by remote executor", jobId);
      }

      return Response.noContent().build();
    }

    return Response.status(Status.NOT_FOUND).build();
  }
}
