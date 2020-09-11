/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.di.domain;

import java.util.Optional;
import org.osgi.framework.ServiceReference;

public interface Registry<T> {

  String ON_ARRIVAL_METHOD = "onArrival";
  String ON_DEPARTURE_METHOD = "onDeparture";
  String FILTER_PREFIX = "(objectClass=";
  String FILTER_SUFFIX = ")";

  interface State<U> {

    Optional<U> get(String... identifiers);

    Optional<U> onArrival(ServiceReference<U> ref);

    Optional<U> onDeparture(ServiceReference<U> ref);
  }

  Registry.State<T> getRegistryState();

  default void onArrival(ServiceReference<T> ref) {
    Optional<T> t = getRegistryState().onArrival(ref);
    onRegister(t);
  }

  default void onDeparture(ServiceReference<T> ref) {
    Optional<T> t = getRegistryState().onDeparture(ref);
    onDeregister(t);
  }

  default void onRegister(Optional<T> instance) {}

  default void onDeregister(Optional<T> instance) {}
}
