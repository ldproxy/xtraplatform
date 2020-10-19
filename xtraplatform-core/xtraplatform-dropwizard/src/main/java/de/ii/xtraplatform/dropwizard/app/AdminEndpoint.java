package de.ii.xtraplatform.dropwizard.app;

import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.codahale.metrics.servlets.PingServlet;
import com.codahale.metrics.servlets.ThreadDumpServlet;
import de.ii.xtraplatform.dropwizard.domain.XtraPlatform;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;

/**
 * @author zahnen
 */
@Component
@Provides(specifications = {AdminEndpoint.class})
@Instantiate
public class AdminEndpoint extends HttpServlet {
    public static final String DEFAULT_HEALTHCHECK_URI = "/healthcheck";
    public static final String DEFAULT_METRICS_URI = "/metrics";
    public static final String DEFAULT_PING_URI = "/ping";
    public static final String DEFAULT_THREADS_URI = "/threads";

    private static final String TEMPLATE = String.format(
            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"%n" +
                    "        \"http://www.w3.org/TR/html4/loose.dtd\">%n" +
                    "<html>%n" +
                    "<head>%n" +
                    "  <title>Operational Menu{10}</title>%n" +
                    "</head>%n" +
                    "<body>%n" +
                    "  <h1>Operational Menu{10}</h1>%n" +
                    "  <ul>%n" +
                    "    <li><a href=\"{0}{1}?pretty=true\">Metrics</a></li>%n" +
                    "    <li><a href=\"{2}{3}\">Ping</a></li>%n" +
                    "    <li><a href=\"{4}{5}\">Threads</a></li>%n" +
                    "    <li><a href=\"{6}{7}?pretty=true\">Healthcheck</a></li>%n" +
                    "  </ul>%n" +
                    "</body>%n" +
                    "</html>"
    );
    private static final String CONTENT_TYPE = "text/html";
    private static final long serialVersionUID = -2850794040708785318L;

    private final HealthCheckServlet healthCheckServlet;
    private final MetricsServlet metricsServlet;
    private final PingServlet pingServlet;
    private final ThreadDumpServlet threadDumpServlet;
    private final String serviceName;

    public AdminEndpoint(@Requires XtraPlatform xtraPlatform) {
        this.serviceName = xtraPlatform.getApplicationName();
        this.healthCheckServlet = new HealthCheckServlet();
        this.metricsServlet = new MetricsServlet();
        this.pingServlet = new PingServlet();
        this.threadDumpServlet = new ThreadDumpServlet();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        healthCheckServlet.init(config);
        metricsServlet.init(config);
        pingServlet.init(config);
        threadDumpServlet.init(config);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String path = req.getContextPath() + req.getServletPath();

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
        resp.setContentType(CONTENT_TYPE);
        try (PrintWriter writer = resp.getWriter()) {
            writer.println(MessageFormat.format(TEMPLATE, path, DEFAULT_METRICS_URI, path, DEFAULT_PING_URI, path,
                    DEFAULT_THREADS_URI, path, DEFAULT_HEALTHCHECK_URI, path, "",
                    serviceName == null ? "" : " (" + serviceName + ")"));
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
