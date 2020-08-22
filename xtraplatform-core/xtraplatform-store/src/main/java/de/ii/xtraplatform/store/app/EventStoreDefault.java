package de.ii.xtraplatform.store.app;

import akka.stream.ActorMaterializer;
import de.ii.xtraplatform.streams.domain.ActorSystemProvider;
import de.ii.xtraplatform.runtime.domain.StoreConfiguration.StoreMode;
import de.ii.xtraplatform.dropwizard.domain.XtraPlatform;
import de.ii.xtraplatform.store.domain.EventStore;
import de.ii.xtraplatform.store.domain.EventStoreDriver;
import de.ii.xtraplatform.store.domain.EventStoreSubscriber;
import de.ii.xtraplatform.store.domain.MutationEvent;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

@Component
@Provides
@Instantiate
public class EventStoreDefault implements EventStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventStoreDefault.class);

    private final EventStoreDriver driver;
    private final EventSubscriptions subscriptions;
    private final boolean isReadOnly;

    EventStoreDefault(@Context BundleContext bundleContext,
                      @Requires XtraPlatform xtraPlatform,
                      @Requires ActorSystemProvider actorSystemProvider,
                      @Requires EventStoreDriver eventStoreDriver) {
        this.driver = eventStoreDriver;
        this.subscriptions = new EventSubscriptions(ActorMaterializer.create(actorSystemProvider.getActorSystem(bundleContext)));
        this.isReadOnly = xtraPlatform.getConfiguration().store.mode == StoreMode.READ_ONLY;
    }

    @Validate
    private void onInit() {
        driver.start();

        driver.loadEventStream()
              .forEach(subscriptions::emitEvent);

        //replay done
        subscriptions.startListening();
    }

    @Override
    public void subscribe(EventStoreSubscriber subscriber) {
        subscriptions.addSubscriber(subscriber);
    }

    @Override
    public void push(MutationEvent event) {
        if (isReadOnly) {
            throw new UnsupportedOperationException("Operating in read-only mode, writes are not allowed.");
        }

        try {
            if (Objects.equals(event.deleted(), true)) {
                driver.deleteAllEvents(event.type(), event.identifier(), event.format());
            } else {
                driver.saveEvent(event);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save event", e);
        }

        subscriptions.emitEvent(event);
    }

    @Override
    public boolean isReadOnly() {
        return isReadOnly;
    }
}
