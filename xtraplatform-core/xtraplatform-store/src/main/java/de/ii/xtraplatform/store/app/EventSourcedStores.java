/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app;

import de.ii.xtraplatform.di.domain.Registry;
import de.ii.xtraplatform.di.domain.RegistryState;
import de.ii.xtraplatform.store.domain.EventSourcedStore;
import java.util.Optional;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Instantiate
@Wbp(
    filter =
        Registry.FILTER_PREFIX + EventSourcedStores.EVENT_SOURCED_STORE + Registry.FILTER_SUFFIX,
    onArrival = Registry.ON_ARRIVAL_METHOD,
    onDeparture = Registry.ON_DEPARTURE_METHOD)
public class EventSourcedStores implements Registry<EventSourcedStore<?>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventSourcedStores.class);
  static final String EVENT_SOURCED_STORE = "de.ii.xtraplatform.store.domain.EventSourcedStore";

  private final BundleContext context;
  private final Registry.State<EventSourcedStore<?>> stores;

  public EventSourcedStores(@Context BundleContext context) {
    this.context = context;
    this.stores = new RegistryState<>(EVENT_SOURCED_STORE, context);
  }

  @Override
  public State<EventSourcedStore<?>> getRegistryState() {
    return stores;
  }

  @Override
  public void onRegister(Optional<EventSourcedStore<?>> instance) {
    LOGGER.info("REGISTERED {}", instance);
  }
}
