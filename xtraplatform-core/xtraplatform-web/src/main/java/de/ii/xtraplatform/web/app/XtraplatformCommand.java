/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.cli.EnvironmentCommand;
import io.dropwizard.core.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * @author zahnen
 */
public class XtraplatformCommand<T extends Configuration> extends EnvironmentCommand<T> {
  static final String CMD = "server";

  private final Class<T> configurationClass;

  public XtraplatformCommand(Application<T> application) {
    super(application, CMD, "Runs the Dropwizard application as an HTTP server");
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
  }
}
