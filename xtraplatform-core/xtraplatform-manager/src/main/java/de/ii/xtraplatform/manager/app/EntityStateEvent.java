/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.manager.app;

import de.ii.xtraplatform.store.domain.entities.EntityState;
import de.ii.xtraplatform.streams.domain.Event;
import java.util.function.Consumer;
import org.immutables.value.Value;

@Value.Immutable
public interface EntityStateEvent extends Event, EntityState {

  @Override
  default void addListener(Consumer<EntityState> listener) {}
}
