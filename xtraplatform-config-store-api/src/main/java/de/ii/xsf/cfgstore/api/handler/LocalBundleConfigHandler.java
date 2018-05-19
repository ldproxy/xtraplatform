/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.cfgstore.api.handler;

import de.ii.xsf.cfgstore.api.ConfigurationListenerRegistry;
import de.ii.xsf.cfgstore.api.LocalBundleConfigStore;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.annotations.Handler;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.metadata.Element;

import java.util.Dictionary;

/**
 * Created by zahnen on 27.11.15.
 */
@Handler(name = "LocalBundleConfig", namespace = BundleConfigHandler.NAMESPACE)
public class LocalBundleConfigHandler extends BundleConfigHandler {

    @Requires
    private ConfigurationListenerRegistry clr2;

    @Requires
    private LocalBundleConfigStore localStore;

    @Override
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {
        this.annotationName = LocalBundleConfig.class.getSimpleName().toLowerCase();
        super.configure(metadata, configuration);
    }

    @Override
    public void onCreation(Object instance) {
        this.clr = clr2;
        this.store = localStore;
        super.onCreation(instance);
    }
}
