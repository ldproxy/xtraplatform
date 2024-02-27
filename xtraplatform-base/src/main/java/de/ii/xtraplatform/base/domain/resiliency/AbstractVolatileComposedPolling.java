/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain.resiliency;

import de.ii.xtraplatform.base.domain.resiliency.Volatile2.Polling;
import de.ii.xtraplatform.base.domain.util.Tuple;
import java.util.Objects;

public abstract class AbstractVolatileComposedPolling extends AbstractVolatileComposed
    implements VolatileComposed, Polling {

  protected AbstractVolatileComposedPolling(
      VolatileRegistry volatileRegistry, String... capabilities) {
    super(volatileRegistry, capabilities);
  }

  @Override
  protected void onVolatileStarted() {
    poll();
    super.onVolatileStarted();
  }

  @Override
  public final void poll() {
    for (String subKey : this.getComponents()) {
      if (getComponent(subKey) instanceof Polling
          && getComponent(subKey) instanceof AbstractVolatile) {
        AbstractVolatile abstractVolatile = (AbstractVolatile) getComponent(subKey);
        Polling polling = (Polling) getComponent(subKey);

        Tuple<State, String> result = polling.check();

        if (!Objects.equals(result.first(), abstractVolatile.getState())) {
          abstractVolatile.setState(result.first());
          abstractVolatile.setMessage(result.second());
        }
      }
    }
  }
}
