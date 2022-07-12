/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.codahale.metrics.servlets.PingServlet;
import com.codahale.metrics.servlets.ThreadDumpServlet;
import com.github.azahnen.dagger.annotations.AutoBind;
import dagger.Lazy;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.web.domain.AdminSubEndpoint;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class AdminEndpoint extends HttpServlet implements AdminEndpointServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(AdminEndpoint.class);

  private static final String TEMPLATE =
      String.format(
          "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"%n"
              + "        \"http://www.w3.org/TR/html4/loose.dtd\">%n"
              + "<html>%n"
              + "<head>%n"
              + "  <title>Operational Menu{10}</title>%n"
              + "</head>%n"
              + "<body>%n"
              + "  <h1>Operational Menu{10}</h1>%n"
              + "  <ul>%n"
              + "    <li><a href=\"{0}{1}?pretty=true\">Metrics</a></li>%n"
              + "    <li><a href=\"{2}{3}\">Ping</a></li>%n"
              + "    <li><a href=\"{4}{5}\">Threads</a></li>%n"
              + "    <li><a href=\"{6}{7}?pretty=true\">Healthcheck</a></li>%n"
              + "{9}"
              + "  </ul>%n"
              + "</body>%n"
              + "</html>");
  private static final String CONTENT_TYPE = "text/html";
  private static final long serialVersionUID = -2850794040708785318L;

  private final HealthCheckServlet healthCheckServlet;
  private final MetricsServlet metricsServlet;
  private final PingServlet pingServlet;
  private final ThreadDumpServlet threadDumpServlet;
  private final String serviceName;
  private final Lazy<Set<AdminSubEndpoint>> subEndpoints;

  @Inject
  public AdminEndpoint(AppContext appContext, Lazy<Set<AdminSubEndpoint>> subEndpoints) {
    this.serviceName = appContext.getName();
    this.healthCheckServlet = new HealthCheckServlet();
    this.metricsServlet = new MetricsServlet();
    this.pingServlet = new PingServlet();
    this.threadDumpServlet = new ThreadDumpServlet();
    this.subEndpoints = subEndpoints;
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    healthCheckServlet.init(config);
    metricsServlet.init(config);
    pingServlet.init(config);
    threadDumpServlet.init(config);

    for (AdminSubEndpoint adminSubEndpoint : subEndpoints.get()) {
      adminSubEndpoint.getServlet().init(config);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    final String path = req.getContextPath() + req.getServletPath();

    String subEndpointLinks = "";
    for (AdminSubEndpoint adminSubEndpoint : subEndpoints.get()) {
      if (adminSubEndpoint.getLabel().isPresent()) {
        subEndpointLinks +=
            String.format(
                "    <li><a href=\"%s%s\">%s</a></li>%n",
                path, adminSubEndpoint.getPath(), adminSubEndpoint.getLabel().get());
      }
    }

    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
    resp.setContentType(CONTENT_TYPE);
    try (PrintWriter writer = resp.getWriter()) {
      writer.println(
          MessageFormat.format(
              TEMPLATE,
              path,
              DEFAULT_METRICS_URI,
              path,
              DEFAULT_PING_URI,
              path,
              DEFAULT_THREADS_URI,
              path,
              DEFAULT_HEALTHCHECK_URI,
              path,
              subEndpointLinks,
              serviceName == null ? "" : " (" + serviceName + ")"));
    }
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    final String uri = req.getPathInfo();
    if (uri == null || uri.equals("/")) {
      super.service(req, resp);
    } else if (uri.equals(DEFAULT_HEALTHCHECK_URI)) {
      healthCheckServlet.service(req, resp);
    } else if (uri.startsWith(DEFAULT_METRICS_URI)) {
      metricsServlet.service(req, resp);
    } else if (uri.equals(DEFAULT_PING_URI)) {
      pingServlet.service(req, resp);
    } else if (uri.equals(DEFAULT_THREADS_URI)) {
      threadDumpServlet.service(req, resp);
    } else {
      Optional<AdminSubEndpoint> subEndpoint =
          subEndpoints.get().stream()
              .filter(endpoint -> Objects.equals(endpoint.getPath(), uri))
              .findFirst();
      if (subEndpoint.isPresent()) {
        subEndpoint.get().getServlet().service(req, resp);
      } else {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      }
    }
  }
}
