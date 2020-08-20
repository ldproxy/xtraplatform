package de.ii.xtraplatform.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.server.DefaultServerFactory;

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE, defaultImpl = XtraPlatformServerFactory.class)
public class XtraPlatformServerFactory extends DefaultServerFactory {

    private String externalUrl;

    @JsonProperty
    public String getExternalUrl() {
        return externalUrl;
    }

    @JsonProperty
    public void setExternalUrl(final String externalUrl) {
        this.externalUrl = externalUrl;
    }
}
