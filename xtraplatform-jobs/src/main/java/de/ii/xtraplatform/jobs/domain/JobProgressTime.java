/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.domain;

import java.util.concurrent.atomic.AtomicLong;
import org.immutables.value.Value;

@Value.Immutable
public interface JobProgressTime extends JobProgress {
  @Value.Default
  default AtomicLong getStartedAt() {
    return new AtomicLong(-1);
  }

  @Value.Default
  default AtomicLong getUpdatedAt() {
    return new AtomicLong(-1);
  }
}
