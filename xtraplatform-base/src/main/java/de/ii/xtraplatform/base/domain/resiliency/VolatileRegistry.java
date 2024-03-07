/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain.resiliency;

import de.ii.xtraplatform.base.domain.resiliency.Volatile2.State;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface VolatileRegistry {

  @FunctionalInterface
  interface ChangeHandler {
    void change(State from, State to);
  }

  void register(Volatile2 dependency);

  void unregister(Volatile2 dependency);

  void change(Volatile2 dependency, State from, State to);

  Runnable watch(Volatile2 dependency, ChangeHandler handler);

  void onAvailable(Runnable runnable, Volatile2... volatiles);

  void listen(BiConsumer<String, Volatile2> onRegister, Consumer<String> onUnRegister);
}
