/*
 * Copyright 2025 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.infra;

import de.ii.xtraplatform.base.domain.StoreSource;
import de.ii.xtraplatform.entities.domain.EntityEvent;
import de.ii.xtraplatform.entities.domain.EventStoreDriver;
import java.util.List;
import java.util.stream.Stream;

public class EventStoreDriverAdHoc implements EventStoreDriver {

  private final List<EntityEvent> events;

  public EventStoreDriverAdHoc(List<EntityEvent> events) {
    this.events = events;
  }

  @Override
  public String getType() {
    return "ADHOC";
  }

  @Override
  public boolean isAvailable(StoreSource storeSource) {
    return true;
  }

  @Override
  public Stream<EntityEvent> load(StoreSource storeSource) {
    return events.stream();
  }
}
