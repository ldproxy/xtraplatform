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
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.jobs.domain.Job;
import de.ii.xtraplatform.jobs.domain.JobQueue;
import de.ii.xtraplatform.jobs.domain.JobSet;
import de.ii.xtraplatform.jobs.domain.JobSet.JobSetDetails;
import de.ii.xtraplatform.ops.domain.OpsEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.LinkedHashMap;
import java.util.List;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/api/jobs")
@Singleton
@AutoBind
public class OpsEndpointJobs implements OpsEndpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpsEndpointJobs.class);

  private final JobQueue jobQueue;
  private final ObjectMapper objectMapper;

  public class JobResponse {
    public List<JobSet> sets;
    public List<Job> open;
  }

  @Inject
  public OpsEndpointJobs(Jackson jackson, JobQueue jobQueue) {
    this.objectMapper = jackson.getDefaultObjectMapper();
    this.jobQueue = jobQueue;
  }

  @Override
  public String getEntrypoint() {
    return "jobs";
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get all jobs",
      description =
          "Returns a set containing one or more jobs along with their respective progress statuses.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content =
                @Content(
                    mediaType = "application/json",
                    schema =
                        @Schema(
                            implementation = JobResponse.class,
                            example = "{\n  \"sets\" : [ ]\n}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public Response getJobs(@QueryParam("debug") boolean debug) throws JsonProcessingException {
    try {
      Map<String, Object> jobs = new LinkedHashMap<>();
      jobs.put("sets", jobQueue.getSets());

      if (debug) {
        jobs.put("open", jobQueue.getOpen());
        jobs.put("taken", jobQueue.getTaken());
        jobs.put("failed", jobQueue.getFailed());
      }

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
  @Operation(summary = "Take a job", description = "Takes a job from the queue")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Job taken successfully"),
        @ApiResponse(responseCode = "204", description = "No content"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public Response takeJob(Map<String, String> executor) throws JsonProcessingException {
    synchronized (this) {
      Optional<Job> job = jobQueue.take(executor.get("type"), executor.get("id"));

      if (job.isEmpty()) {
        return Response.noContent().build();
      }

      return processJobTaken(job.get(), executor.get("id"));
    }
  }

  private Response processJobTaken(Job job, String executorId) throws JsonProcessingException {
    logJobTaken(job, executorId);

    Optional<JobSet> jobSet = job.getPartOf().map(jobQueue::getSet);
    setupJobSetContext(jobSet);
    handleJobSetStartup(job, jobSet);

    return Response.ok(objectMapper.writeValueAsString(job)).build();
  }

  private void logJobTaken(Job job, String executorId) {
    if (LOGGER.isTraceEnabled() || LOGGER.isTraceEnabled(MARKER.JOBS)) {
      LOGGER.trace(MARKER.JOBS, "Job {} taken by remote executor {}", job.getId(), executorId);
    }
  }

  private void setupJobSetContext(Optional<JobSet> jobSet) {
    if (jobSet.isPresent() && jobSet.get().getEntity().isPresent()) {
      LogContext.put(LogContext.CONTEXT.SERVICE, jobSet.get().getEntity().get());
    }
  }

  private void handleJobSetStartup(Job job, Optional<JobSet> jobSet) {
    if (jobSet.isPresent()
        && !jobSet.get().isStarted()
        && (jobSet.get().getSetup().isEmpty()
            || !Objects.equals(job.getId(), jobSet.get().getSetup().get().getId()))) {

      jobQueue.startJobSet(jobSet.get());
      logJobSetStarted(jobSet.get());
    }
  }

  private void logJobSetStarted(JobSet jobSet) {
    if (LOGGER.isInfoEnabled() || LOGGER.isInfoEnabled(MARKER.JOBS)) {
      LOGGER.info(
          MARKER.JOBS,
          "{} started ({})",
          jobSet.getLabel(),
          jobQueue.getJobSetDetails(JobSetDetails.class, jobSet).getLabel());
    }
  }

  @POST
  @Path("/{jobId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Update a job", description = "Updates the progress of a job")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "No content"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public Response updateJob(@PathParam("jobId") String jobId, Map<String, Object> progress) {
    synchronized (this) {
      try {
        Optional<Job> job =
            jobQueue.getTaken().stream()
                .filter(job1 -> Objects.equals(job1.getId(), jobId))
                .findFirst();

        if (job.isPresent()) {
          int delta = (Integer) progress.getOrDefault("delta", 0);

          jobQueue.updateJob(job.get(), delta);

          if (delta > 0 && job.get().getPartOf().isPresent()) {
            JobSet set = jobQueue.getSet(job.get().getPartOf().get());
            jobQueue.updateJobSet(set, delta, progress);
          }

          if (LOGGER.isTraceEnabled() || LOGGER.isTraceEnabled(MARKER.JOBS)) {
            LOGGER.trace(
                MARKER.JOBS, "Job {} progress updated by remote executor ({})", jobId, progress);
          }
        } else {
          if (LOGGER.isWarnEnabled() || LOGGER.isWarnEnabled(MARKER.JOBS)) {
            LOGGER.warn(MARKER.JOBS, "Received progress update for unknown job {}", jobId);
          }
        }
      } catch (Throwable e) {
        LOGGER.error("Error while updating job {}", jobId, e);
      }

      return Response.noContent().build();
    }
  }

  @DELETE
  @Path("/{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Close a job", description = "Closes a job and marks it as done or error")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "No content"),
        @ApiResponse(responseCode = "404", description = "Job not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public Response closeJob(
      @PathParam("jobId") String jobId, @Parameter(hidden = true) Map<String, String> result) {
    synchronized (this) {
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
}
