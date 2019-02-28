/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.dropwizard.cfg;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class XtraServerFrameworkConfiguration extends Configuration {
    //@NotEmpty
    //@JsonProperty
    //public String configDir;
    
    @JsonProperty
    public boolean useFormattedJsonOutput;

    @JsonProperty
    public boolean allowServiceReAdding;
    
    @JsonProperty
    public String externalURL;
    
    @JsonProperty
    public int maxDebugLogDurationMinutes = 60;
    
    @Valid
    @NotNull
    @JsonProperty
    public HttpClientConfiguration httpClient = new HttpClientConfiguration();
    
}