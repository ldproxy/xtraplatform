/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import dagger.Lazy;
import de.ii.xtraplatform.base.domain.AppConfiguration;
import de.ii.xtraplatform.web.domain.DropwizardPlugin;
import de.ii.xtraplatform.web.domain.ServletRegistration;
import de.ii.xtraplatform.web.domain.StaticResourceHandler;
import de.ii.xtraplatform.web.domain.StaticResources;
import io.dropwizard.core.setup.Environment;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Servlet;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
@AutoBind
public class StaticPlugin implements DropwizardPlugin, StaticResourceHandler {

  private final Lazy<Set<StaticResources>> staticResources;
  private final Lazy<Set<ServletRegistration>> servletRegistrations;
  private final Map<String, Servlet> servlets;

  @Inject
  public StaticPlugin(
      Lazy<Set<StaticResources>> staticResources,
      Lazy<Set<ServletRegistration>> servletRegistrations) {
    this.staticResources = staticResources;
    this.servletRegistrations = servletRegistrations;
    this.servlets = new HashMap<>();
  }

  @Override
  public void init(AppConfiguration configuration, Environment environment) {

    staticResources
        .get()
        .forEach(
            staticResources1 -> {
              if (!staticResources1.isEnabled()) {
                return;
              }

              StaticResourceServlet servlet =
                  new StaticResourceServlet(
                      staticResources1.getResourcePath(),
                      staticResources1.getUrlPath(),
                      null,
                      staticResources1
                          .getResourceReader()
                          .orElse(new StaticResourceReaderJar(staticResources1.getClass())));

              Dynamic registration =
                  environment.servlets().addServlet(staticResources1.getUrlPath(), servlet);
              registration.addMapping(getUrlPattern(staticResources1.getUrlPath()));

              servlets.put(staticResources1.getUrlPath(), servlet);
            });

    servletRegistrations
        .get()
        .forEach(
            servletRegistration -> {
              Dynamic registration =
                  environment
                      .servlets()
                      .addServlet(servletRegistration.getUrlPath(), servletRegistration);
              registration.addMapping(getUrlPattern(servletRegistration.getUrlPath()));
            });
  }

  @Override
  public boolean handle(String path, HttpServletRequest request, HttpServletResponse response) {
    for (String prefix : servlets.keySet()) {
      if (path.startsWith(prefix) || ("/" + path).startsWith(prefix)) {
        try {
          servlets.get(prefix).service(request, response);
          return true;
        } catch (Throwable e) {
          return false;
        }
      }
    }

    return false;
  }

  private String getUrlPattern(String path) {
    return path.endsWith("/*") ? path : path.endsWith("/") ? path + "*" : path + "/*";
  }
}
