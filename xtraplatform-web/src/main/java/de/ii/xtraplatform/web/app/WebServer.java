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
import io.dropwizard.core.setup.Environment;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jetty.server.Server;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class WebServer implements AppLifeCycle, DropwizardPlugin {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(WebServer.class);

  private final AppContext appContext;

  private final CompletableFuture<Server> server;

  @Inject
  public WebServer(AppContext appContext) {
    this.appContext = appContext;
    this.server = new CompletableFuture<>();
  }

  @Override
  public int getPriority() {
    if (appContext.getConfiguration().getModules().isStartupAsync()) {
      // start after DropwizardProvider
      return 1;
    }
    // start last
    return 2000;
  }

  @Override
  public void init(AppConfiguration configuration, Environment environment) {
    server.complete(environment.getApplicationContext().getServer());
  }

  @Override
  public CompletionStage<Void> onStart(boolean isStartupAsync) {
    try {
      server.get().start();

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Started web server at {}", appContext.getUri());
      }
    } catch (Throwable ex) {
      LogContext.error(LOGGER, ex, "Error starting {}", appContext.getName());
      return CompletableFuture.failedFuture(ex);
    }

    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void onStop() {
    try {
      server.get().stop();
      server.get().join();
    } catch (Throwable e) {
      LogContext.error(LOGGER, e, "Error when stopping web server");
    }
  }
}
