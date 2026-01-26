/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import java.util.function.Consumer;

public interface EntityState {

  enum STATE {
    UNKNOWN,
    LOADING,
    RELOADING,
    DISABLED,
    DEFECTIVE,
    ACTIVE,
    HEALTHY
  }

  String getId();

  String getEntityType();

  STATE getEntityState();

  STATE getPreviousState();

  void addListener(Consumer<EntityState> listener);

  default boolean isActive() {
    return getEntityState() == STATE.ACTIVE
        || getEntityState() == STATE.RELOADING && getPreviousState() == STATE.ACTIVE;
  }
}
