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
package de.ii.xsf.cfgstore.api;

import de.ii.xsf.configstore.api.KeyValueStore;
import de.ii.xsf.configstore.api.rest.AbstractGenericResourceStore;
import de.ii.xsf.core.util.json.DeepUpdater;
import de.ii.xsf.dropwizard.api.Jackson;
import de.ii.xsf.logging.XSFLogger;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class LocalBundleConfigStoreDefault extends AbstractGenericResourceStore<JsonBundleConfig, LocalBundleConfigStore> implements LocalBundleConfigStore {
    
    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(LocalBundleConfigStoreDefault.class);
    
    public static final String STORE_ID = "local-bundle-config-store";    
    
    //@Requires
    //ConfigurableComponentFactory coi;
    
    public LocalBundleConfigStoreDefault(@Requires Jackson jackson, @Requires KeyValueStore rootConfigStore) {
        super(rootConfigStore, STORE_ID, false, new DeepUpdater<JsonBundleConfig>(jackson.getDefaultObjectMapper()), new JsonBundleConfigSerializer(jackson.getDefaultObjectMapper()));
    }

    @Override
    protected JsonBundleConfig createEmptyResource() {
        return new JsonBundleConfig("",null);
    }
    
    @Validate
    public void start() {
        //coi.reconfigure();
    }
}
