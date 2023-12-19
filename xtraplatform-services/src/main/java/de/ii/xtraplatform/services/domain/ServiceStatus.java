/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.services.domain;

import de.ii.xtraplatform.entities.domain.EntityState;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public interface ServiceStatus extends ServiceData {

  enum STATUS {
    STARTED,
    STOPPED
  }

  EntityState.STATE getStatus();

  @Value.Default
  default boolean getHasBackgroundTask() {
    return false;
  }

  @Value.Default
  default boolean getHasProgress() {
    return false;
  }

  @Value.Default
  default int getProgress() {
    return 0;
  }

  @Nullable
  String getMessage();
}
