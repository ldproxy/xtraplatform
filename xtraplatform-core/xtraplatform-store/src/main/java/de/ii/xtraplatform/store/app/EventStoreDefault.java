/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app;

import de.ii.xtraplatform.dropwizard.domain.XtraPlatform;
import de.ii.xtraplatform.runtime.domain.StoreConfiguration;
import de.ii.xtraplatform.runtime.domain.StoreConfiguration.StoreMode;
import de.ii.xtraplatform.store.domain.EntityEvent;
import de.ii.xtraplatform.store.domain.EventFilter;
import de.ii.xtraplatform.store.domain.EventStore;
import de.ii.xtraplatform.store.domain.EventStoreDriver;
import de.ii.xtraplatform.store.domain.EventStoreSubscriber;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.ImmutableIdentifier;
import de.ii.xtraplatform.store.domain.ImmutableReloadEvent;
import de.ii.xtraplatform.store.domain.ImmutableReplayEvent;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaultsStore;
import de.ii.xtraplatform.streams.domain.ActorSystemProvider;
import de.ii.xtraplatform.streams.domain.StreamRunner;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Provides
@Instantiate
public class EventStoreDefault implements EventStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventStoreDefault.class);

  private final EventStoreDriver driver;
  private final EventSubscriptions subscriptions;
  private final StoreConfiguration storeConfiguration;
  private final boolean isReadOnly;

  EventStoreDefault(
      @Context BundleContext bundleContext,
      @Requires XtraPlatform xtraPlatform,
      @Requires ActorSystemProvider actorSystemProvider,
      @Requires EventStoreDriver eventStoreDriver) {
    this.driver = eventStoreDriver;
    this.subscriptions =
        new EventSubscriptions(new StreamRunner(bundleContext, actorSystemProvider, "events"));
    this.storeConfiguration = xtraPlatform.getConfiguration().store;
    this.isReadOnly = storeConfiguration.mode == StoreMode.READ_ONLY;
  }

  @Validate
  private void onInit() {
    LOGGER.info("Store mode: {}", storeConfiguration.mode);

    driver.start();

    driver.loadEventStream().forEach(subscriptions::emitEvent);

    // replay done
    subscriptions.startListening();

    if (storeConfiguration.watch && driver.supportsWatch()) {
      LOGGER.info("Watching store for changes");
      new Thread(
              () ->
                  driver.startWatching(
                      changedFiles -> {
                        LOGGER.info("Store changes detected: {}", changedFiles);
                        EventFilter filter = EventFilter.fromPaths(changedFiles);
                        // LOGGER.debug("FILTER {}", filter);
                        replay(filter);
                      }))
          .start();
    }
  }

  @Override
  public void subscribe(EventStoreSubscriber subscriber) {
    subscriptions.addSubscriber(subscriber);
  }

  @Override
  public void push(EntityEvent event) {
    if (isReadOnly) {
      throw new UnsupportedOperationException(
          "Operating in read-only mode, writes are not allowed.");
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

  @Override
  public void replay(EventFilter filter) {
    Set<EntityEvent> deleteEvents = new HashSet<>();

    List<EntityEvent> eventStream =
        driver
            .loadEventStream()
            .filter(
                event -> {
                  boolean matches = filter.matches(event);

                  if (matches) {
                    /*LOGGER.debug("ALLOW {type: {}, path: {}, id: {}}", event.type(), event.identifier()
                    .path(), event.identifier()
                                  .id());*/
                    if (Objects.equals(event.type(), "entities")
                        || Objects.equals(event.type(), "overrides")) {
                      String id =
                          event.identifier().path().size() > 1
                              ? event.identifier().path().get(1)
                              : event.identifier().id();
                      boolean deleted =
                          deleteEvents.add(
                              ImmutableReplayEvent.builder()
                                  .type("entities")
                                  .deleted(true)
                                  .identifier(Identifier.from(id, event.identifier().path().get(0)))
                                  .payload(ValueEncodingJackson.YAML_NULL)
                                  .build());
                      /*if (deleted) {
                        LOGGER.debug("DELETING {} {}", event.identifier()
                                                            .path()
                                                            .get(0), id);
                      }*/
                    } else {
                      String id = EntityDataDefaultsStore.EVENT_TYPE;
                      boolean deleted =
                          deleteEvents.add(
                              ImmutableReplayEvent.builder()
                                  .type(EntityDataDefaultsStore.EVENT_TYPE)
                                  .deleted(true)
                                  .identifier(
                                      ImmutableIdentifier.builder()
                                          .id(id)
                                          .path(event.identifier().path())
                                          .build())
                                  .payload(ValueEncodingJackson.YAML_NULL)
                                  .build());
                      /*if (deleted) {
                        LOGGER.debug("DELETING {} {}", event.identifier()
                                                            .path(), id);
                      }*/
                    }
                    return true;
                  }
                  /*LOGGER.debug("SKIP {type: {}, path: {}, id: {}}", event.type(), event.identifier()
                  .path(), event.identifier()
                                .id());*/
                  return false;
                })
            .collect(Collectors.toList());

    deleteEvents.forEach(
        event -> {
          subscriptions.emitEvent(event).join();
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
            // ignore
          }
        });
    eventStream.forEach(
        event -> {
          subscriptions.emitEvent(event).join();
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
            // ignore
          }
        });
    // TODO: type
    subscriptions
        .emitEvent(ImmutableReloadEvent.builder().type("entities").filter(filter).build())
        .join();
  }
}
