/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.domain;

import java.util.concurrent.atomic.AtomicInteger;
import org.immutables.value.Value;

@Value.Immutable
public interface JobProgress {
  @Value.Default
  default AtomicInteger getTotal() {
    return new AtomicInteger(-1);
  }

  @Value.Default
  default AtomicInteger getCurrent() {
    return new AtomicInteger(0);
  }

  default int getPercent() {
    int total = getTotal().get();

    if (total == -1) {
      return 0;
    }
    if (total == 0) {
      return 100;
    }

    int current = getCurrent().get();

    if (current >= total) {
      return 100;
    }

    return (int) ((((float) Math.max(current, 0)) / total) * 100);
  }

  default boolean isDone() {
    return getPercent() == 100;
  }
}
