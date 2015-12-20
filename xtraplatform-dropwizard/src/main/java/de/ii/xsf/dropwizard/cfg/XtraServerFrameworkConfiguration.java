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