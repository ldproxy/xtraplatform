/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.logging.LoggingFactory;
import io.dropwizard.server.ServerFactory;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class AppConfiguration extends Configuration {

  @Valid @NotNull private ServerConfiguration server;
  @Valid @NotNull private LoggingConfiguration logging;
  // TODO: not used anymore, but removing breaks backwards compatibility
  @Deprecated @JsonProperty public boolean useFormattedJsonOutput;
  @Deprecated @JsonProperty public boolean allowServiceReAdding;
  @Valid @NotNull private HttpClientConfiguration httpClient;
  @Valid @NotNull @JsonProperty public StoreConfiguration store;
  @Valid @NotNull @JsonProperty public AuthConfig auth;
  @Valid @NotNull @JsonProperty public ManagerConfiguration manager;
  @Valid @NotNull @JsonProperty public BackgroundTasksConfiguration backgroundTasks;
  @Valid @NotNull @JsonProperty public ProjConfiguration proj;
  @Valid @JsonProperty public ClusterConfiguration cluster;

  public AppConfiguration() {
    this.logging = new LoggingConfiguration();
    this.server = new ServerConfiguration();
    this.httpClient = new HttpClientConfiguration();
    this.store = new StoreConfiguration();
    this.auth = new AuthConfig();
    this.manager = new ManagerConfiguration();
    this.backgroundTasks = new BackgroundTasksConfiguration();
    this.proj = new ProjConfiguration();
  }

  public AppConfiguration(boolean noInit) {
  }

  @Override
  @JsonProperty("server")
  public ServerConfiguration getServerFactory() {
    return server;
  }

  @Override
  @JsonIgnore
  public void setServerFactory(ServerFactory factory) {}

  @JsonProperty("server")
  public void setServerFactory(ServerConfiguration factory) {
    this.server = factory;
  }

  @Override
  @JsonIgnore
  public synchronized LoggingFactory getLoggingFactory() {
    return logging;
  }

  @JsonProperty("logging")
  public synchronized LoggingConfiguration getLoggingConfiguration() {
    return logging;
  }

  @Override
  @JsonIgnore
  public synchronized void setLoggingFactory(LoggingFactory factory) {}

  @JsonProperty("logging")
  public synchronized void setLoggingFactory(LoggingConfiguration factory) {
    this.logging = factory;
  }

  @JsonProperty("httpClient")
  public HttpClientConfiguration getHttpClient() {
    return httpClient;
  }

  @JsonProperty("httpClient")
  public void setHttpClient(HttpClientConfiguration httpClient) {
    this.httpClient = httpClient;
  }
}
