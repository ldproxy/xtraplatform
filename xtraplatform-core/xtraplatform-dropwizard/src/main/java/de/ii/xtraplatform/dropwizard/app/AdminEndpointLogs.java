package de.ii.xtraplatform.dropwizard.app;

import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.dropwizard.domain.AdminSubEndpoint;
import de.ii.xtraplatform.dropwizard.domain.Jackson;
import de.ii.xtraplatform.runtime.domain.ThirdPartyLoggingFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Provides
@Instantiate
public class AdminEndpointLogs implements AdminSubEndpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(AdminEndpointLogs.class);

  private final HttpServlet servlet;
  private final ObjectMapper objectMapper;
  private final LoggerContext loggerContext;

  public AdminEndpointLogs(@Requires Jackson jackson) {
    this.objectMapper = jackson.getDefaultObjectMapper();
    this.servlet = new LogsServlet();
    this.loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
  }

  @Override
  public String getPath() {
    return "/logs";
  }

  @Override
  public HttpServlet getServlet() {
    return servlet;
  }

  class LogsServlet extends HttpServlet {
    private static final long serialVersionUID = 3772654177231086757L;
    private static final String CONTENT_TYPE = "application/json";
    private static final String CACHE_CONTROL = "Cache-Control";
    private static final String NO_CACHE = "must-revalidate,no-cache,no-store";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setHeader(CACHE_CONTROL, NO_CACHE);
      resp.setContentType(CONTENT_TYPE);

      String level = loggerContext.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME)
          .getLevel().toString();

      Optional<ThirdPartyLoggingFilter> optionalThirdPartyLoggingFilter = loggerContext.getTurboFilterList().stream()
          .filter(turboFilter -> turboFilter instanceof ThirdPartyLoggingFilter).map(turboFilter -> (ThirdPartyLoggingFilter)turboFilter).findFirst();

      try (PrintWriter writer = resp.getWriter()) {
        objectMapper.writeValue(writer, getLogInfo(level, optionalThirdPartyLoggingFilter));
      }
    }

    private ImmutableMap<String, Object> getLogInfo(String level, Optional<ThirdPartyLoggingFilter> optionalThirdPartyLoggingFilter) {
      return ImmutableMap.of(
              "level", level,
              "filter", getFilterInfo(optionalThirdPartyLoggingFilter)
          );
    }

    private ImmutableMap<String, Boolean> getFilterInfo(Optional<ThirdPartyLoggingFilter> optionalThirdPartyLoggingFilter) {
      return optionalThirdPartyLoggingFilter
          .map(thirdPartyLoggingFilter -> ImmutableMap.of(
              "sqlQueries", thirdPartyLoggingFilter.isSqlQueries(),
              "sqlResults", thirdPartyLoggingFilter.isSqlResults(),
              "configDumps", thirdPartyLoggingFilter.isConfigDumps(),
              "stackTraces", thirdPartyLoggingFilter.isStackTraces()
          ))
          .orElse(ImmutableMap.of());
    }
  }
}
