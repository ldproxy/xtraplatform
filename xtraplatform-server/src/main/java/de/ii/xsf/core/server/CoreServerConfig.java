package de.ii.xsf.core.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.ii.xsf.cfgstore.api.BundleConfigDefault;
import de.ii.xsf.cfgstore.api.handler.LocalBundleConfig;
import java.io.IOException;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

/**
 *
 * @author zahnen
 */
//@LocalConfiguration
@Component
@Provides(specifications = {CoreServerConfig.class})
@Instantiate
@LocalBundleConfig
public class CoreServerConfig extends BundleConfigDefault {

    private String externalUrl;

    public CoreServerConfig() {
        // set default values
        this.externalUrl = "bla";
    }

    @JsonProperty
    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    @Override
    public void save() throws IOException {
        setExternalUrl("notbla");
        super.save();
    }
}
