/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.dropwizard.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.logging.ConsoleAppenderFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Objects;

//TODO: cleanup
public class XtraPlatformConfiguration extends Configuration {

    @Valid
    @NotNull
    private ServerFactory server;

    public XtraPlatformConfiguration() {
        DefaultServerFactory defaultServerFactory = new DefaultServerFactory();
        LogbackAccessRequestLogFactory logbackAccessRequestLogFactory = new LogbackAccessRequestLogFactory();
        logbackAccessRequestLogFactory.setAppenders(ImmutableList.of());
        defaultServerFactory.setRequestLogFactory(logbackAccessRequestLogFactory);
        this.server = defaultServerFactory;
    }

    @Override
    @JsonProperty("server")
    public ServerFactory getServerFactory() {
        return server;
    }

    @Override
    @JsonProperty("server")
    public void setServerFactory(ServerFactory factory) {
        if (factory instanceof DefaultServerFactory) {
            DefaultServerFactory defaultServerFactory = (DefaultServerFactory) factory;
            if (defaultServerFactory.getRequestLogFactory() instanceof LogbackAccessRequestLogFactory) {
                LogbackAccessRequestLogFactory logbackAccessRequestLogFactory = (LogbackAccessRequestLogFactory) defaultServerFactory.getRequestLogFactory();
                if (logbackAccessRequestLogFactory.getAppenders().size() == 1 && logbackAccessRequestLogFactory.getAppenders().get(0) instanceof ConsoleAppenderFactory) {

                    logbackAccessRequestLogFactory.setAppenders(ImmutableList.of());
                    //defaultServerFactory.setRequestLogFactory(logbackAccessRequestLogFactory);
                    //this.server = defaultServerFactory;
                }
                //return;
            }
        }
        this.server = factory;
    }


    //TODO: not used anymore, but removing breaks backwards compatibility
    @JsonProperty
    public boolean useFormattedJsonOutput;

    @JsonProperty
    public boolean allowServiceReAdding;
    /*
    @JsonProperty
    public String externalURL;
    
    @JsonProperty
    public int maxDebugLogDurationMinutes = 60;
    */
    @Valid
    @NotNull
    @JsonProperty
    public HttpClientConfiguration httpClient = new HttpClientConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    public StoreConfiguration store = new StoreConfiguration();

    @Valid
    @JsonProperty
    public ClusterConfiguration cluster;
    
}