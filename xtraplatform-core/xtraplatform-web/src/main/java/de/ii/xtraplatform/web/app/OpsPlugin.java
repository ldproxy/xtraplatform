/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppConfiguration;
import de.ii.xtraplatform.web.domain.DropwizardPlugin;
import io.dropwizard.core.setup.Environment;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jetty.servlet.ServletHolder;

@Singleton
@AutoBind
public class OpsPlugin implements DropwizardPlugin {

  private final AdminEndpointServlet adminEndpoint;

  @Inject
  OpsPlugin(AdminEndpointServlet adminEndpoint) {
    this.adminEndpoint = adminEndpoint;
  }

  @Override
  public void init(AppConfiguration configuration, Environment environment) {
    ServletHolder[] admin = environment.getAdminContext().getServletHandler().getServlets();

    int ai = -1;
    for (int i = 0; i < admin.length; i++) {
      if (admin[i].getName().contains("Admin")) {
        ai = i;
      }
    }
    if (ai >= 0) {
      String name = admin[ai].getName();
      admin[ai] = new ServletHolder(adminEndpoint);
      admin[ai].setName(name);

      environment.getAdminContext().getServletHandler().setServlets(admin);
    }
  }
}
