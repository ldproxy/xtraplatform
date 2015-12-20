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
