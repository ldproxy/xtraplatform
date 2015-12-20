package de.ii.xsf.cfgstore.api.handler;

import de.ii.xsf.cfgstore.api.ConfigurationListenerRegistry;
import de.ii.xsf.cfgstore.api.SynchronizedBundleConfigStore;
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
