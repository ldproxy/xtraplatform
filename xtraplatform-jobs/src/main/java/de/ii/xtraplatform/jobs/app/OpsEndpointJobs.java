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
import de.ii.xtraplatform.jobs.domain.Job;
import de.ii.xtraplatform.jobs.domain.JobQueue;
import de.ii.xtraplatform.ops.domain.OpsEndpoint;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
            "sets", jobQueue.getSets(), "open", jobQueue.getOpen(), "taken", jobQueue.getTaken());

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
    LOGGER.debug("Taking job: {}", executor);

    Optional<Job> job = jobQueue.take(executor.get("type"), executor.get("id"));

    if (job.isPresent()) {
      return Response.ok(objectMapper.writeValueAsString(job.get())).build();
    }

    return Response.noContent().build();
  }

  // TODO: id in path?
  @DELETE
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public synchronized Response closeJob(Map<String, String> jobRef) throws JsonProcessingException {
    LOGGER.debug("Closing job: {}", jobRef);

    if (jobQueue.done(jobRef.get("id"))) {
      return Response.noContent().build();
    }

    return Response.status(Status.NOT_FOUND).build();
  }
}
