package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.akka.ActorSystemProvider;
import de.ii.xtraplatform.dropwizard.api.StoreConfiguration.StoreMode;
import de.ii.xtraplatform.dropwizard.api.XtraPlatform;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Controller;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Provides
@Instantiate
public class ReadOnlyEventStore extends AbstractFileSystemEventStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadOnlyEventStore.class);

    @ServiceController(value = false)
    private boolean publish;
    private final boolean isEnabled;

    ReadOnlyEventStore(@Context BundleContext bundleContext, @Requires XtraPlatform xtraPlatform,
                       @Requires ActorSystemProvider actorSystemProvider) {
        super(bundleContext, xtraPlatform, actorSystemProvider, xtraPlatform.getConfiguration().store.mode == StoreMode.READ_ONLY);
        this.isEnabled = xtraPlatform.getConfiguration().store.mode == StoreMode.READ_ONLY;
    }

    @Validate
    private void onInit() {
        if (isEnabled) {
            replay();
            this.publish = true;
        }
    }

    @Override
    public void push(MutationEvent event) {
        throw new UnsupportedOperationException("Operating in read-only mode, writes are not allowed.");
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }
}
