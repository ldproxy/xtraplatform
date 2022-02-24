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
import de.ii.xtraplatform.web.domain.Http;
import de.ii.xtraplatform.web.domain.HttpClient;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.setup.Environment;
import java.net.URI;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class HttpApache implements Http, DropwizardPlugin {

  private HttpClient defaultClient;

  @Inject
  HttpApache() {}

  @Override
  public void init(AppConfiguration appConfiguration, Environment environment) {
    this.defaultClient =
        new HttpClientApache(
            new HttpClientBuilder(environment)
                .using(appConfiguration.getHttpClient())
                .build("foo"));
  }

  @Override
  public HttpClient getDefaultClient() {
    return defaultClient;
  }

  @Override
  public HttpClient getHostClient(URI host, int maxParallelRequests, int idleTimeout) {
    return defaultClient;
  }
}
