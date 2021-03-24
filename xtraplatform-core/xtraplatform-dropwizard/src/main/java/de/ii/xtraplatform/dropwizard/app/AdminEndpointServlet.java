/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.dropwizard.app;

import de.ii.xtraplatform.dropwizard.domain.AdminSubEndpoint;
import java.io.Serializable;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import org.osgi.framework.ServiceReference;

public interface AdminEndpointServlet extends Servlet, ServletConfig, Serializable {

  String DEFAULT_HEALTHCHECK_URI = "/healthcheck";
  String DEFAULT_METRICS_URI = "/metrics";
  String DEFAULT_PING_URI = "/ping";
  String DEFAULT_THREADS_URI = "/threads";

  void onArrival(ServiceReference<AdminSubEndpoint> ref);

  void onDeparture(ServiceReference<AdminSubEndpoint> ref);

  @Override
  void init(ServletConfig config) throws ServletException;
}
