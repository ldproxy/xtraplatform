/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.server;

import com.google.common.base.Strings;
import de.ii.xtraplatform.cfgstore.api.BundleConfigDefault;
import de.ii.xtraplatform.cfgstore.api.ConfigPropertyDescriptor;
import de.ii.xtraplatform.cfgstore.api.handler.LocalBundleConfig;
import de.ii.xtraplatform.dropwizard.api.ConfigurationProvider;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.Objects;
import java.util.Optional;

/**
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@LocalBundleConfig(category = "Webserver", properties = {
        @ConfigPropertyDescriptor(name = CoreServerConfigImpl.EXTERNAL_URL, label = "External URL", defaultValue = CoreServerConfigImpl.DEFAULT_URL, uiType = ConfigPropertyDescriptor.UI_TYPE.URL),
        @ConfigPropertyDescriptor(name = "port", label = "Port", defaultValue = "7080", hidden = true)
})
public class CoreServerConfigImpl extends BundleConfigDefault implements CoreServerConfig {

    static final String DEFAULT_URL = "http://localhost:7080/rest/services";
    static final String EXTERNAL_URL = "externalUrl";

    private final Optional<String> externalUrlFromConfig;

    public CoreServerConfigImpl(@Requires ConfigurationProvider configurationProvider) {
        this.externalUrlFromConfig = Optional.ofNullable(configurationProvider.getConfiguration()
                                                                         .getServerFactory()
                                                                         .getExternalUrl());
    }

    @Override
    public String getExternalUrl() {
        String path =  Strings.nullToEmpty(properties.get(EXTERNAL_URL));

        if ( externalUrlFromConfig.isPresent() && Objects.equals(path, DEFAULT_URL)) {
            return externalUrlFromConfig.get();
        }

        if (!path.startsWith("http")) {
            return "http://hostname" + path;
        }

        return path;
    }

    public void setExternalUrl(String externalUrl) {
        properties.put(EXTERNAL_URL, externalUrl);
    }
}
