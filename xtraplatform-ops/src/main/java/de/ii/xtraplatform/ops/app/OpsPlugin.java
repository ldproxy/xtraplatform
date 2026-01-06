/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ops.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppConfiguration;
import de.ii.xtraplatform.web.domain.DropwizardPlugin;
import io.dropwizard.core.setup.Environment;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletHolder.Wrapper;
import org.eclipse.jetty.servlet.ServletMapping;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

@Singleton
@AutoBind
public class OpsPlugin implements DropwizardPlugin {

  private final OpsRequestDispatcher opsRequestDispatcher;
  private ServletHolder tasksServlet;

  @Inject
  OpsPlugin(OpsRequestDispatcher opsRequestDispatcher) {
    this.opsRequestDispatcher = opsRequestDispatcher;
  }

  @Override
  public void init(AppConfiguration configuration, Environment environment) {
    ServletHandler servletHandler = environment.getAdminContext().getServletHandler();
    ServletHolder[] holders = servletHandler.getServlets();
    ServletMapping[] mappings = servletHandler.getServletMappings();
    List<ServletHolder> newServlets = new ArrayList<>();
    List<ServletMapping> newMappings = new ArrayList<>();

    for (ServletHolder servletHolder : holders) {
      if (servletHolder.getName().contains("Admin")) {
        Servlet adminServlet = getServlet(environment);

        String name = servletHolder.getName();
        ServletHolder ops = new ServletHolder(adminServlet);
        ops.setName(name);
        newServlets.add(ops);
      } else if (Objects.equals(servletHolder.getName(), "tasks")) {
        tasksServlet = servletHolder;
      } else {
        newServlets.add(servletHolder);
      }
    }
    for (ServletMapping servletMapping : mappings) {
      if (!Objects.equals(servletMapping.getServletName(), "tasks")) {
        newMappings.add(servletMapping);
      }
    }

    servletHandler.setServlets(newServlets.toArray(new ServletHolder[] {}));
    servletHandler.setServletMappings(newMappings.toArray(new ServletMapping[] {}));
  }

  private Servlet getServlet(Environment environment) {
    OpsResourceConfig jerseyConfig = new OpsResourceConfig();
    jerseyConfig.setContextPath(environment.getAdminContext().getContextPath());
    ServletContainer adminServlet = new ServletContainer(jerseyConfig);

    jerseyConfig.register(new CorsFilter());
    jerseyConfig.register(opsRequestDispatcher);

    return new Wrapper(adminServlet) {
      @Override
      public void init(ServletConfig config) throws ServletException {
        super.init(config);
        opsRequestDispatcher.init(config, tasksServlet);
      }
    };
  }

  private static class OpsResourceConfig extends ResourceConfig {
    private String urlPattern;
    private String contextPath;

    public OpsResourceConfig() {
      super();
      this.urlPattern = "/*";
      this.contextPath = "/";

      this.property("jersey.config.server.wadl.disableWadl", Boolean.TRUE);
    }

    public String getUrlPattern() {
      return this.urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
      this.urlPattern = urlPattern;
    }

    public String getContextPath() {
      return this.contextPath;
    }

    public void setContextPath(String contextPath) {
      this.contextPath = contextPath;
    }
  }
}
