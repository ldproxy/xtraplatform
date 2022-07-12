/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.cli.ServerCommand;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
public class XtraplatformCommand<T extends Configuration> extends EnvironmentCommand<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerCommand.class);

  private final Class<T> configurationClass;
  private Server server;

  public XtraplatformCommand(Application<T> application) {
    super(application, "server", "Runs the Dropwizard application as an HTTP server");
    this.configurationClass = application.getConfigurationClass();
  }

  /*
   * Since we don't subclass ServerCommand, we need a concrete reference to the configuration
   * class.
   */
  @Override
  protected Class<T> getConfigurationClass() {
    return configurationClass;
  }

  @Override
  protected void run(Environment environment, Namespace namespace, T configuration)
      throws Exception {
    // this is needed if run is not blocking
    cleanupAsynchronously();

    /*System.out.println("COMMAND");
    if (server != null) {
            server.addLifeCycleListener(new LifeCycleListener());
            cleanupAsynchronously();
    }
    else {
        LOGGER.error("Server configuration missing, shutting down");
    }*/
  }

  private class LifeCycleListener extends AbstractLifeCycle.AbstractLifeCycleListener {

    @Override
    public void lifeCycleStopped(LifeCycle event) {
      cleanup();
    }
  }

  public void setServer(Server server) {
    this.server = server;
  }
}
