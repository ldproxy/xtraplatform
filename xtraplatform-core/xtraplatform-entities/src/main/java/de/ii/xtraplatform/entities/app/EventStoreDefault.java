/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.app;

import static de.ii.xtraplatform.entities.domain.EntityDataStore.entityType;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.Lists;
import dagger.Lazy;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.Store;
import de.ii.xtraplatform.base.domain.StoreFilters;
import de.ii.xtraplatform.base.domain.StoreSource;
import de.ii.xtraplatform.base.domain.StoreSource.Content;
import de.ii.xtraplatform.base.domain.StoreSource.Mode;
import de.ii.xtraplatform.base.domain.StoreSourceFsV3;
import de.ii.xtraplatform.entities.domain.EntityDataDefaultsStore;
import de.ii.xtraplatform.entities.domain.EntityEvent;
import de.ii.xtraplatform.entities.domain.EventFilter;
import de.ii.xtraplatform.entities.domain.EventStore;
import de.ii.xtraplatform.entities.domain.EventStoreDriver;
import de.ii.xtraplatform.entities.domain.EventStoreSubscriber;
import de.ii.xtraplatform.entities.domain.ImmutableEventFilter;
import de.ii.xtraplatform.entities.domain.ImmutableIdentifier;
import de.ii.xtraplatform.entities.domain.ImmutableReloadEvent;
import de.ii.xtraplatform.entities.domain.ImmutableReplayEvent;
import de.ii.xtraplatform.streams.domain.Reactive;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

  private final Store store;
  private final Lazy<Set<EventStoreDriver>> drivers;
  private final EventSubscriptions subscriptions;
  private Optional<StoreSource> writableSource;
  private final boolean isReadOnly;

  @Inject
  EventStoreDefault(Store store, Lazy<Set<EventStoreDriver>> drivers, Reactive reactive) {
    this.store = store;
    this.drivers = drivers;
    this.subscriptions = new EventSubscriptionsImpl(reactive.runner("events"));
    this.writableSource = Optional.empty();
    this.isReadOnly = !store.isWritable();
  }

  public EventStoreDefault(
      Store store, EventStoreDriver eventStoreDriver, EventSubscriptions subscriptions) {
    this.store = store;
    this.drivers = () -> Set.of(eventStoreDriver);
    this.subscriptions = subscriptions;
    this.isReadOnly = !store.isWritable();
  }

  @Override
  public int getPriority() {
    return 20;
  }

  @Override
  public void onStart() {
    EventFilter startupFilter = getStartupFilter();
    List<StoreSource> sources = findSources();

    this.writableSource =
        Lists.reverse(sources).stream().filter(source -> source.getMode() == Mode.RW).findFirst();

    sources.forEach(
        source -> {
          Optional<EventStoreDriver> driver =
              findDriver(
                  source,
                  source.getContent() != Content.ALL && !StoreSourceFsV3.isOldDefaultStore(source));

          driver.ifPresent(eventStoreDriver -> load(source, eventStoreDriver, startupFilter));
        });

    // replay done
    subscriptions.startListening();

    if (store.isWatchable()) {
      LOGGER.info("Watching store for changes");

      sources.stream()
          .filter(StoreSource::isWatchable)
          .forEach(
              source -> {
                Optional<EventStoreDriver> driver = findDriver(source, false);

                driver
                    .filter(EventStoreDriver::canWatch)
                    .ifPresent(eventStoreDriver -> watch(source, eventStoreDriver));
              });
    }
  }

  private void load(StoreSource storeSource, EventStoreDriver driver, EventFilter startupFilter) {
    driver
        .load(storeSource)
        .peek(
            event -> {
              if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                    "{} {}", startupFilter.matches(event) ? "Loading" : "Skipping", event.asPath());
              }
            })
        .filter(startupFilter::matches)
        .forEach(subscriptions::emitEvent);
  }

  private void watch(StoreSource storeSource, EventStoreDriver driver) {
    if (store.isWatchable() && storeSource.isWatchable() && driver.canWatch()) {
      // TODO: executor
      new Thread(
              () ->
                  driver
                      .watcher()
                      .listen(
                          storeSource,
                          changedFiles -> {
                            LOGGER.info("Store changes detected: {}", changedFiles);
                            EventFilter replayFilter = EventFilter.fromPaths(changedFiles);

                            if (LOGGER.isTraceEnabled()) {
                              LOGGER.trace("Replay filter {}", replayFilter);
                            }

                            replay(replayFilter);
                          }))
          .start();
    }
  }

  @Override
  public void subscribe(EventStoreSubscriber subscriber) {
    subscriptions.addSubscriber(subscriber);
  }

  private Optional<EventStoreDriver> findDriver(StoreSource storeSource, boolean warn) {
    final boolean[] foundUnavailable = {false};

    // TODO: content all/entities
    Optional<EventStoreDriver> driver =
        drivers.get().stream()
            .filter(d -> Objects.equals(d.getType(), storeSource.getType()))
            .filter(
                d -> {
                  if (!d.isAvailable(storeSource)) {
                    if (warn) {
                      LOGGER.warn("Store source {} not found.", storeSource.getLabel());
                    }
                    foundUnavailable[0] = true;
                    return false;
                  }
                  return true;
                })
            .findFirst();

    if (driver.isEmpty() && !foundUnavailable[0]) {
      LOGGER.error("No driver found for source {}.", storeSource.getLabel());
    }

    return driver;
  }

  private List<StoreSource> findSources() {
    return store.get().stream()
        .filter(
            source ->
                source.getContent() == Content.ALL
                    || source.getContent() == Content.ENTITIES
                    || source.getContent() == Content.DEFAULTS
                    || source.getContent() == Content.INSTANCES
                    || source.getContent() == Content.INSTANCES_OLD
                    || source.getContent() == Content.OVERRIDES)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public void push(EntityEvent event) {
    if (isReadOnly) {
      LOGGER.warn("Store is operating in read-only mode, write operations are not allowed.");
      return;
    }
    if (writableSource.isEmpty()) {
      LOGGER.warn("Ignoring write event for '{}', no writable source found.", event.asPath());
      return;
    }

    Optional<EventStoreDriver> driver = findDriver(writableSource.get(), false);

    if (driver.isEmpty()) {
      LOGGER.warn(
          "Ignoring write event for '{}', no driver found for source {}.",
          event.asPath(),
          writableSource.get().getLabel());
      return;
    }
    if (!driver.get().canWrite()) {
      throw new UnsupportedOperationException(
          String.format(
              "Store driver %s is read-only, write operations are not supported.",
              driver.get().getType()));
    }

    try {
      if (Objects.equals(event.deleted(), true)) {
        driver
            .get()
            .writer()
            .deleteAll(writableSource.get(), event.type(), event.identifier(), event.format());
      } else {
        driver.get().writer().push(writableSource.get(), event);
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
    findSources()
        .forEach(
            source -> {
              Optional<EventStoreDriver> driver = findDriver(source, false);

              driver.ifPresent(eventStoreDriver -> reload(source, eventStoreDriver, filter));
            });

    // TODO: type
    subscriptions.emitEvent(ImmutableReloadEvent.builder().type("entities").filter(filter).build());
  }

  private void reload(StoreSource storeSource, EventStoreDriver driver, EventFilter filter) {
    Set<EntityEvent> deleteEvents = new HashSet<>();

    List<EntityEvent> eventStream =
        driver
            .load(storeSource)
            .filter(
                event -> {
                  boolean matches = filter.matches(event);

                  if (matches) {
                    if (LOGGER.isTraceEnabled()) {
                      LOGGER.trace("ALLOW {}", event.asPath());
                    }
                    if (Objects.equals(event.type(), "entities")
                        || Objects.equals(event.type(), "overrides")) {
                      boolean deleted =
                          deleteEvents.add(
                              ImmutableReplayEvent.builder()
                                  .type("entities")
                                  .deleted(true)
                                  .identifier(event.identifier())
                                  .payload(ValueEncodingJackson.YAML_NULL)
                                  .build());
                      if (deleted && LOGGER.isTraceEnabled()) {
                        LOGGER.trace(
                            "DELETING {} {}",
                            entityType(event.identifier()),
                            event.identifier().id());
                      }
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
                                          .addPath(event.identifier().id())
                                          .build())
                                  .payload(ValueEncodingJackson.YAML_NULL)
                                  .build());
                      if (deleted && LOGGER.isTraceEnabled()) {
                        LOGGER.trace("DELETING {} {}", event.identifier().path(), id);
                      }
                      // also without id for cases where it is keyPathAlias
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
                    }
                    return true;
                  }
                  if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("SKIP {}", event.asPath());
                  }
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
  }

  private EventFilter getStartupFilter() {
    return ImmutableEventFilter.builder()
        .addEventTypes("entities")
        .entityTypes(
            store
                .getFilter()
                .map(StoreFilters::getEntityTypes)
                .flatMap(l -> l.isEmpty() ? Optional.empty() : Optional.of(l))
                .orElse(List.of("*")))
        .ids(store.getFilter().map(StoreFilters::getEntityIds).orElse(List.of("*")))
        .build();
  }
}
