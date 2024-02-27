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

public abstract class AbstractVolatilePolling extends AbstractVolatile
    implements Volatile2, Polling {

  protected AbstractVolatilePolling(VolatileRegistry volatileRegistry) {
    super(volatileRegistry);
  }

  protected AbstractVolatilePolling(VolatileRegistry volatileRegistry, String uniqueKey) {
    super(volatileRegistry, uniqueKey);
  }

  @Override
  public final void poll() {
    Tuple<State, String> result = check();

    if (!Objects.equals(result.first(), getState())) {
      setMessage(result.second());
      setState(result.first());
    }
  }
}
