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
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
            "/dashboard", "/", null, new StaticResourceReaderJar(this.getClass()));
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
  @Path("/api/info")
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
                + "\",\n"
                + "  \"status\": \"HEALTHY\"\n"
                + "}")
        .build();
  }

  @GET
  @Path("/api/health")
  public void getHealth(@Context HttpServletRequest request, @Context HttpServletResponse response)
      throws ServletException, IOException {
    CorsFilter.addCorsHeaders(response);
    healthCheckServlet.service(request, response);
  }

  @GET
  @Path("/api/metrics")
  public void getMetrics(@Context HttpServletRequest request, @Context HttpServletResponse response)
      throws ServletException, IOException {
    CorsFilter.addCorsHeaders(response);
    metricsServlet.service(request, response);
  }

  @GET
  @Path("/api/ping")
  public void getPing(@Context HttpServletRequest request, @Context HttpServletResponse response)
      throws ServletException, IOException {
    CorsFilter.addCorsHeaders(response);
    pingServlet.service(request, response);
  }

  @GET
  @Path("/api/threads")
  public void getThreads(@Context HttpServletRequest request, @Context HttpServletResponse response)
      throws ServletException, IOException {
    CorsFilter.addCorsHeaders(response);
    threadDumpServlet.service(request, response);
  }

  @POST
  @Path("/api/tasks/{task}")
  public void postTasks(@Context HttpServletRequest request, @Context HttpServletResponse response)
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
  @Path("/{path: .+\\.(?:html|js|css|json|woff2)}")
  public void getFile(
      @PathParam("path") String path,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response)
      throws ServletException, IOException {
    LOGGER.debug("FILE {}", path);
    staticServlet.service(request, response);
  }

  @GET
  @Path("/{path: .+}")
  public void getRoute(
      @PathParam("path") String path,
      @Context HttpServletRequest request,
      @Context HttpServletResponse response)
      throws ServletException, IOException {
    LOGGER.debug("ROUTE {}", path);
    if (!path.contains(".") && request instanceof Request) {
      ((Request) request).setServletPathMapping(null);
      ((Request) request)
          .setContext(
              ((Request) request).getContext(), ((Request) request).getPathInContext() + ".html");
    }
    LOGGER.debug("FILE {}", request.getPathInfo());
    staticServlet.service(request, response);
  }
}
