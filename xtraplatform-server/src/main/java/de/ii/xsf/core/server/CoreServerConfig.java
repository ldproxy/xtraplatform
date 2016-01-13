/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
