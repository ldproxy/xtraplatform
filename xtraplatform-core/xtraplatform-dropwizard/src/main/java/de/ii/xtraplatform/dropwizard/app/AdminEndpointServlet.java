package de.ii.xtraplatform.dropwizard.app;

import de.ii.xtraplatform.dropwizard.domain.AdminSubEndpoint;
import org.osgi.framework.ServiceReference;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.Serializable;

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
