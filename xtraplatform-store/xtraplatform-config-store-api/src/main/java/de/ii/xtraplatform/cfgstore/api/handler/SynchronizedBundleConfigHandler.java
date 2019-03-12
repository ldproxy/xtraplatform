/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cfgstore.api.handler;

import de.ii.xtraplatform.cfgstore.api.ConfigurationListenerRegistry;
import de.ii.xtraplatform.cfgstore.api.SynchronizedBundleConfigStore;
import org.apache.felix.ipojo.annotations.Handler;
import org.apache.felix.ipojo.annotations.Requires;

/**
 * Created by zahnen on 28.11.15.
 */
@Handler(name = "SynchronizedBundleConfig", namespace = BundleConfigHandler.NAMESPACE)
public class SynchronizedBundleConfigHandler extends BundleConfigHandler{
    @Requires
    private ConfigurationListenerRegistry clr2;

    @Requires
    private SynchronizedBundleConfigStore store2;

    @Override
    public void onCreation(Object instance) {
        this.clr = clr2;
        this.store = store2;
        super.onCreation(instance);
    }
}
