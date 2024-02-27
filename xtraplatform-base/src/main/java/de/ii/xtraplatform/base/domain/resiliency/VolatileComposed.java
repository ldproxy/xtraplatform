/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain.resiliency;

import com.codahale.metrics.health.HealthCheck;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry.ChangeHandler;
import de.ii.xtraplatform.base.domain.util.Tuple;
import java.util.Optional;
import java.util.Set;

public interface VolatileComposed extends Volatile2 {

  Set<String> getVolatileCapabilities();

  State getState(String capability);

  Optional<String> getMessage(String capability);

  Runnable onStateChange(String capability, ChangeHandler handler, boolean initialCall);

  default boolean isAvailable(String capability) {
    return getState(capability) == State.AVAILABLE;
  }

  default boolean isUsable(String capability) {
    return isAvailable(capability) || getState(capability) == State.LIMITED;
  }

  @Override
  default Optional<HealthCheck> asHealthCheck() {
    return Optional.of(HealthChecks.composed(this));
  }

  static VolatileComposed polling(VolatileRegistry volatileRegistry, String uniqueKey) {
    return new AbstractVolatileComposedPolling(volatileRegistry, uniqueKey) {
      @Override
      public int getIntervalMs() {
        return 0;
      }

      @Override
      public Tuple<State, String> check() {
        return null;
      }
    };
  }
}
