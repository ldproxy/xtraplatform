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
package de.ii.xsf.cfgstore.api.handler;

import de.ii.xsf.cfgstore.api.ConfigurationListenerRegistry;
import de.ii.xsf.cfgstore.api.LocalBundleConfigStore;
import org.apache.felix.ipojo.annotations.Handler;
import org.apache.felix.ipojo.annotations.Requires;

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
    public void onCreation(Object instance) {
        this.clr = clr2;
        this.store = localStore;
        super.onCreation(instance);
    }
}
