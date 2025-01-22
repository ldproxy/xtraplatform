/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ops.app;

import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.codahale.metrics.servlets.PingServlet;
import com.codahale.metrics.servlets.ThreadDumpServlet;
import com.github.azahnen.dagger.annotations.AutoBind;
import dagger.Lazy;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.ops.domain.OpsEndpoint;
import de.ii.xtraplatform.web.domain.StaticResourceReaderJar;
import de.ii.xtraplatform.web.domain.StaticResourceServlet;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@AutoBind
public class OpsRequestDispatcherImpl implements OpsRequestDispatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpsRequestDispatcherImpl.class);

  private final AppContext appContext;
  private final HealthCheckServlet healthCheckServlet;
  private final MetricsServlet metricsServlet;
  private final PingServlet pingServlet;
  private final ThreadDumpServlet threadDumpServlet;
  private final StaticResourceServlet staticServlet;
  private final Lazy<Set<OpsEndpoint>> subEndpoints;
  private Servlet tasksServlet;
  private final Reader reader;
  private final OpenAPI openAPI;

  @Inject
  OpsRequestDispatcherImpl(AppContext appContext, Lazy<Set<OpsEndpoint>> subEndpoints) {
    this.appContext = appContext;
    this.healthCheckServlet = new HealthCheckServlet();
    this.metricsServlet = new MetricsServlet();
    this.pingServlet = new PingServlet();
    this.threadDumpServlet = new ThreadDumpServlet();
    this.subEndpoints = subEndpoints;

    this.staticServlet =
        new StaticResourceServlet(
            "/dashboard",
            "/",
            null,
            new StaticResourceReaderJar(this.getClass()),
            Set.of("txt", "html"));

    ModelConverters.getInstance().addConverter(new CustomModelConverter());

    this.reader = new Reader(new OpenAPI());
    Set<Class<?>> endpointClasses =
        subEndpoints.get().stream().map(OpsEndpoint::getClass).collect(Collectors.toSet());
    endpointClasses.add(this.getClass());
    this.openAPI = reader.read(endpointClasses);

    Info info =
        new Info()
            .title("Dashboard API")
            .version("1.0.0")
            .description("This is an example description for the API.");
    this.openAPI.setInfo(info);
  }

  @Override
  public void init(ServletConfig servletConfig, ServletHolder tasksServlet) {
    try {
      healthCheckServlet.init(servletConfig);
      metricsServlet.init(servletConfig);
      pingServlet.init(servletConfig);
      threadDumpServlet.init(servletConfig);

      tasksServlet.start();
      tasksServlet.initialize();
      this.tasksServlet = tasksServlet.getServletInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @GET
  @Path("/api")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getApi() throws IOException {

    String json = Json.pretty().writeValueAsString(openAPI);

    return Response.ok(json).build();
    /*
    return Response.ok(
            Resources.toByteArray(Resources.getResource(this.getClass(), "/openapi.json")))
        .build(); */
  }

  public class AppInfo {
    @Schema(description = "Application name", example = "ldproxy")
    public String name;

    @Schema(description = "Application version", example = "4.3.0-SNAPSHOT")
    public String version;

    @Schema(description = "Application URL", example = "http://localhost:7080/")
    public String url;

    @Schema(description = "Application environment", example = "DEVELOPMENT")
    public String env;
  }

  @GET
  @Path("/api/info")
  @Operation(
      summary = "Get application info",
      description = "Returns the application's name, version, URL, and environment")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AppInfo.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public Response getInfo() {
    return Response.ok(
            "{\n"
                + "  \"name\": \""
                + appContext.getName()
                + "\",\n"
                + "  \"version\": \""
                + appContext.getVersion()
                + "\",\n"
                + "  \"url\": \""
                + appContext.getUri()
                + "\",\n"
                + "  \"env\": \""
                + appContext.getEnvironment()
                + "\"\n"
                // + "\",\n"
                // + "  \"status\": \"HEALTHY\"\n"
                + "}")
        .build();
  }

  public class HealthResponse {
    @Schema(
        description = "Components health status",
        example =
            "{\"app/crs\":{\"healthy\":true,\"duration\":0,\"state\":\"AVAILABLE\",\"timestamp\":\"2025-01-16T11:39:12.721+01:00\"}}")
    public Map<String, ComponentHealth> components;

    public static class ComponentHealth {
      @Schema(description = "Health status", example = "true")
      public boolean healthy;

      @Schema(description = "Duration in milliseconds", example = "0")
      public int duration;

      @Schema(description = "State of the component", example = "AVAILABLE")
      public String state;

      @Schema(
          description = "Timestamp of the health check",
          example = "2025-01-16T11:39:12.721+01:00")
      public String timestamp;

      @Schema(description = "Capabilities of the component")
      public Capabilities capabilities;

      @Schema(description = "Nested components health status")
      public Map<String, ComponentHealth> components;
    }

    public static class Capabilities {
      @Schema(description = "Read capability")
      public Capability read;

      @Schema(description = "Write capability")
      public Capability write;
    }

    public static class Capability {
      @Schema(description = "Health status", example = "true")
      public boolean healthy;

      @Schema(description = "State of the capability", example = "AVAILABLE")
      public String state;
    }
  }

  @GET
  @Path("/api/health")
  @Operation(
      summary = "Get health status",
      description = "Returns the health status of various components")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = HealthResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public void getHealth(@Context HttpServletRequest request, @Context HttpServletResponse response)
      throws ServletException, IOException {
    CorsFilter.addCorsHeaders(response);
    healthCheckServlet.service(request, response);
  }

  public class MetricsResponse {
    public String version;
    public Map<String, Gauge> gauges;
    public Map<String, Counter> counters;
    public Map<String, Histogram> histograms;
    public Map<String, Meter> meters;
    public Map<String, Timer> timers;

    public static class Gauge {
      @Schema(oneOf = {String.class, Integer.class})
      public Object value;
    }

    public static class Counter {
      public long count;
    }

    public static class Histogram {
      public long count;
      public double value;
    }

    public static class Meter {
      public long count;
      public double value;
    }

    public static class Timer {
      public long count;
      public double value;
    }
  }

  @GET
  @Path("/api/metrics")
  @Operation(summary = "Get metrics", description = "Returns the metrics of the application")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MetricsResponse.class))),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public void getMetrics(@Context HttpServletRequest request, @Context HttpServletResponse response)
      throws ServletException, IOException {
    CorsFilter.addCorsHeaders(response);
    metricsServlet.service(request, response);
  }

  public enum PingResponse {
    PONG
  }

  @GET
  @Path("/api/ping")
  @Operation(
      summary = "Ping the server",
      description = "Returns a simple pong response to check if the server is running")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content =
                @Content(
                    mediaType = "text/plain",
                    schema = @Schema(implementation = PingResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public void getPing(@Context HttpServletRequest request, @Context HttpServletResponse response)
      throws ServletException, IOException {
    CorsFilter.addCorsHeaders(response);
    pingServlet.service(request, response);
  }

  @GET
  @Path("/api/threads")
  @Operation(summary = "Get thread dump", description = "Returns a thread dump of the server")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content =
                @Content(
                    mediaType = "text/plain",
                    schema =
                        @Schema(
                            example =
                                "\"dw-admin-105\" id=105 state=TIMED_WAITING - waiting on <0x04941bd8> (a java.util.concurrent.SynchronousQueue$Transferer) - locked <0x04941bd8> (a java.util.concurrent.SynchronousQueue$Transferer) at java.base@21.0.5/jdk.internal.misc.Unsafe.park(Native Method) at java.base@21.0.5/java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:410) at java.base@21.0.5/java.util.concurrent.LinkedTransferQueue$DualNode.await(LinkedTransferQueue.java:452) at java.base@21.0.5/java.util.concurrent.SynchronousQueue$Transferer.xferLifo(SynchronousQueue.java:194) at java.base@21.0.5/java.util.concurrent.SynchronousQueue.xfer(SynchronousQueue.java:235) at java.base@21.0.5/java.util.concurrent.SynchronousQueue.poll(SynchronousQueue.java:338) at app/de.ii.xtraplatform.runtime.tpl@6.3.0-SNAPSHOT/org.eclipse.jetty.util.thread.ReservedThreadExecutor$ReservedThread.reservedWait(ReservedThreadExecutor.java:325) at app/de.ii.xtraplatform.runtime.tpl@6.3.0-SNAPSHOT/org.eclipse.jetty.util.thread.ReservedThreadExecutor$ReservedThread.run(ReservedThreadExecutor.java:401) at app/de.ii.xtraplatform.runtime.tpl@6.3.0-SNAPSHOT/org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:969) at app/de.ii.xtraplatform.runtime.tpl@6.3.0-SNAPSHOT/org.eclipse.jetty.util.thread.QueuedThreadPool$Runner.doRunJob(QueuedThreadPool.java:1194) at app/de.ii.xtraplatform.runtime.tpl@6.3.0-SNAPSHOT/org.eclipse.jetty.util.thread.QueuedThreadPool$Runner.run(QueuedThreadPool.java:1149) at java.base@21.0.5/java.lang.Thread.runWith(Thread.java:1596) at java.base@21.0.5/java.lang.Thread.run(Thread.java:1583)"))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public void getThreads(@Context HttpServletRequest request, @Context HttpServletResponse response)
      throws ServletException, IOException {
    CorsFilter.addCorsHeaders(response);
    threadDumpServlet.service(request, response);
  }

  @POST
  @Path("/api/tasks/{task}")
  @Operation(summary = "Post a task", description = "Posts a task to the server")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Task posted successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public void postTasks(
      @PathParam("task") String task,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response)
      throws ServletException, IOException {
    CorsFilter.addCorsHeaders(response);
    if (request instanceof Request) {
      ((Request) request).setServletPathMapping(null);
      ((Request) request)
          .setContext(
              ((Request) request).getContext(),
              ((Request) request).getPathInContext().replace("/api/tasks", ""));
    }
    tasksServlet.service(request, response);
  }

  @Path("/api/{entrypoint: [a-z]+}")
  @Operation(
      summary = "Get sub-endpoint",
      description = "Returns the sub-endpoint based on the entrypoint")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Successful operation"),
        @ApiResponse(responseCode = "404", description = "Sub-endpoint not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public OpsEndpoint getOther(@PathParam("entrypoint") String entrypoint) {
    Optional<OpsEndpoint> subEndpoint =
        subEndpoints.get().stream()
            .filter(endpoint -> Objects.equals(endpoint.getEntrypoint(), entrypoint))
            .findFirst();

    if (subEndpoint.isEmpty()) {
      throw new NotFoundException();
    }

    return subEndpoint.get();
  }

  @GET
  @Path("/{path: .+\\.(?:html|js|css|json|woff2|txt)}")
  @Operation(summary = "Get file", description = "Returns a static file based on the path")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Successful operation"),
        @ApiResponse(responseCode = "404", description = "File not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public void getFile(
      @PathParam("path") String path,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response)
      throws ServletException, IOException {
    // LOGGER.debug("FILE {}", path);
    staticServlet.service(request, response);
  }

  @GET
  @Path("/{path: .*}")
  @Operation(summary = "Get route", description = "Returns a route based on the path")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Successful operation"),
        @ApiResponse(responseCode = "404", description = "Route not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public void getRoute(
      @PathParam("path") String path,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response)
      throws ServletException, IOException {
    // LOGGER.debug("ROUTE {}", path);
    if (!path.contains(".") && request instanceof Request) {
      String newPath =
          path.isBlank() ? "/index.html" : ((Request) request).getPathInContext() + ".html";

      ((Request) request).setServletPathMapping(null);
      ((Request) request).setContext(((Request) request).getContext(), newPath);
    }
    // LOGGER.debug("FILE {}", request.getPathInfo());
    staticServlet.service(request, response);
  }
}
