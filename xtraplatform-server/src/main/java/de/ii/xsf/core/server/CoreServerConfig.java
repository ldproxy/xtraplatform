/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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
