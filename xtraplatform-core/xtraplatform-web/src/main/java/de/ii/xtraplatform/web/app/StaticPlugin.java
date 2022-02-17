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
import de.ii.xtraplatform.web.domain.StaticResourceServlet;
import de.ii.xtraplatform.web.domain.StaticResources;
import io.dropwizard.setup.Environment;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class StaticPlugin implements DropwizardPlugin {

  private final Lazy<Set<StaticResources>> staticResources;

  @Inject
  public StaticPlugin(Lazy<Set<StaticResources>> staticResources) {
    this.staticResources = staticResources;
  }

  @Override
  public void init(AppConfiguration configuration, Environment environment) {
    staticResources
        .get()
        .forEach(
            staticResources1 -> {
              environment
                  .servlets()
                  .addServlet(
                      staticResources1.getUrlPath(),
                      new StaticResourceServlet(
                          staticResources1.getResourcePath(),
                          staticResources1.getUrlPath(),
                          null,
                          null));
            });
  }
}
