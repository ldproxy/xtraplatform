/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import com.codahale.metrics.MetricRegistry;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.web.domain.Http;
import de.ii.xtraplatform.web.domain.HttpClient;
import io.dropwizard.client.HttpClientBuilder;
import java.net.URI;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class HttpApache implements Http {

  private final HttpClient defaultClient;

  @Inject
  HttpApache(AppContext appContext) {
    this.defaultClient =
        new HttpClientApache(
            new HttpClientBuilder(new MetricRegistry())
                .using(appContext.getConfiguration().getHttpClient())
                .build("default"));
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
