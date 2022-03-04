/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppConfiguration;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.web.domain.DropwizardPlugin;
import io.dropwizard.setup.Environment;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jetty.server.Server;
import org.slf4j.LoggerFactory;

/** @author zahnen */
@Singleton
@AutoBind
public class WebServer implements AppLifeCycle, DropwizardPlugin {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(WebServer.class);

  private final AppContext appContext;

  private Server server;

  @Inject
  public WebServer(AppContext appContext) {
    this.appContext = appContext;
  }

  @Override
  public int getPriority() {
    // start last
    return 2000;
  }

  @Override
  public void init(AppConfiguration configuration, Environment environment) {
    this.server = environment.getApplicationContext().getServer();
  }

  @Override
  public void onStart() {
    try {
      server.start();

      LOGGER.info("Started web server at {}", appContext.getUri());
    } catch (Throwable ex) {
      LogContext.error(LOGGER, ex, "Error starting {}", appContext.getName());
      System.exit(1);
    }
  }

  @Override
  public void onStop() {
    try {
      server.stop();
      server.join();
    } catch (Exception e) {
      LogContext.error(LOGGER, e, "Error when stopping web server");
    }
  }
}
