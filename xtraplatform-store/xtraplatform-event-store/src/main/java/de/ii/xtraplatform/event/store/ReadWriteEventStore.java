package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.akka.ActorSystemProvider;
import de.ii.xtraplatform.dropwizard.api.StoreConfiguration;
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

import java.io.IOException;
import java.util.Objects;

@Component
@Provides
@Instantiate
public class ReadWriteEventStore extends AbstractFileSystemEventStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadWriteEventStore.class);

    @ServiceController(value = false)
    private boolean publish;
    private final boolean isEnabled;

    ReadWriteEventStore(@Context BundleContext bundleContext, @Requires XtraPlatform xtraPlatform,
                        @Requires ActorSystemProvider actorSystemProvider) {
        super(bundleContext, xtraPlatform, actorSystemProvider, xtraPlatform.getConfiguration().store.mode == StoreMode.READ_WRITE);
        this.isEnabled = xtraPlatform.getConfiguration().store.mode == StoreMode.READ_WRITE;
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
        //store
        try {
            if (Objects.equals(event.deleted(), true)) {
                eventReaderWriter.deleteAllEvents(event.type(), event.identifier(), event.format());
            } else {
                eventReaderWriter.saveEvent(event);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save event", e);
        }

        //emit
        emit(event);
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }
}
