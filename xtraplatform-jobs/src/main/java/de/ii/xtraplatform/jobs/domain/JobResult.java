/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.jobs.domain;

import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface JobResult {

  static JobResult success() {
    return new ImmutableJobResult.Builder().build();
  }

  static JobResult onHold() {
    return new ImmutableJobResult.Builder().onHold(true).build();
  }

  static JobResult retry(String error) {
    return new ImmutableJobResult.Builder().error(error).retry(true).build();
  }

  static JobResult error(String error) {
    return new ImmutableJobResult.Builder().error(error).build();
  }

  Optional<String> getError();

  @Value.Default
  default boolean isRetry() {
    return false;
  }

  @Value.Default
  default boolean isOnHold() {
    return false;
  }

  @Value.Derived
  default boolean isSuccess() {
    return getError().isEmpty() && !isOnHold();
  }

  @Value.Derived
  default boolean isFailure() {
    return getError().isPresent();
  }
}
