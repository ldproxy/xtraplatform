/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain.resiliency;

import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry.ChangeHandler;
import java.util.Optional;

public interface OptionalVolatileCapability<T> extends OptionalCapability<T>, Volatile2 {

  static <T> OptionalVolatileCapability<T> unsupported(Class<T> clazz) {
    return new OptionalVolatileCapability<T>() {
      @Override
      public boolean isSupported() {
        return false;
      }

      @Override
      public T get() {
        return null;
      }

      @Override
      public State getState() {
        return State.UNAVAILABLE;
      }

      @Override
      public Optional<String> getMessage() {
        return Optional.empty();
      }

      @Override
      public Runnable onStateChange(ChangeHandler handler, boolean initialCall) {
        return () -> {};
      }
    };
  }
}
