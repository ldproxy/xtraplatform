/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.server;

import com.google.common.base.Strings;
import de.ii.xsf.cfgstore.api.BundleConfigDefault;
import de.ii.xsf.cfgstore.api.ConfigPropertyDescriptor;
import de.ii.xsf.cfgstore.api.handler.LocalBundleConfig;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.IOException;

/**
 *
 * @author zahnen
 */
@Component
@Provides(specifications = {CoreServerConfig.class})
@Instantiate
@LocalBundleConfig(category = "Webserver Settings", properties = {
        @ConfigPropertyDescriptor(name = CoreServerConfig.EXTERNAL_URL, label = "External URL path", defaultValue = "/rest/services"),
        @ConfigPropertyDescriptor(name = "port", label = "Port", defaultValue = "7080", hidden = true)
})
public class CoreServerConfig extends BundleConfigDefault {

    static final String EXTERNAL_URL = "externalUrl";

    public String getExternalUrl() {
        String path =  Strings.nullToEmpty(properties.get(EXTERNAL_URL));

        if (!path.isEmpty()) {
            return "http://hostname" + path;
        }

        return path;
    }

    public void setExternalUrl(String externalUrl) {
        properties.put(EXTERNAL_URL, externalUrl);
    }
}
