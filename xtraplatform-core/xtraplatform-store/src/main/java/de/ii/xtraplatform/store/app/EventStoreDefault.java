/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.StoreConfiguration;
import de.ii.xtraplatform.base.domain.StoreConfiguration.StoreMode;
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
import de.ii.xtraplatform.streams.domain.Reactive;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class EventStoreDefault implements EventStore, AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventStoreDefault.class);

  private final EventStoreDriver driver;
  private final EventSubscriptions subscriptions;
  private final StoreConfiguration storeConfiguration;
  private final boolean isReadOnly;

  @Inject
  EventStoreDefault(AppContext appContext, EventStoreDriver eventStoreDriver, Reactive reactive) {
    this.driver = eventStoreDriver;
    this.subscriptions = new EventSubscriptionsImpl(reactive.runner("events"));
    this.storeConfiguration = appContext.getConfiguration().store;
    this.isReadOnly = storeConfiguration.mode == StoreMode.READ_ONLY;
  }

  public EventStoreDefault(
      StoreConfiguration storeConfiguration,
      EventStoreDriver eventStoreDriver,
      EventSubscriptions subscriptions) {
    this.driver = eventStoreDriver;
    this.subscriptions = subscriptions;
    this.storeConfiguration = storeConfiguration;
    this.isReadOnly = storeConfiguration.mode == StoreMode.READ_ONLY;
  }

  @Override
  public int getPriority() {
    // start first
    return 1;
  }

  @Override
  public void onStart() {
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
                    // LOGGER.debug("ALLOW {}", event.asPath());
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
                  // LOGGER.debug("SKIP {}", event.asPath());
                  return false;
                })
            // TODO: set priority per event type (for now alphabetic works:
            //  defaults < entities < overrides)
            .sorted(Comparator.naturalOrder())
            .collect(Collectors.toList());

    deleteEvents.forEach(
        event -> {
          subscriptions.emitEvent(event);
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
            // ignore
          }
        });
    eventStream.forEach(
        event -> {
          subscriptions.emitEvent(event);
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
            // ignore
          }
        });
    // TODO: type
    subscriptions.emitEvent(ImmutableReloadEvent.builder().type("entities").filter(filter).build());
  }
}
