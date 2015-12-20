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
