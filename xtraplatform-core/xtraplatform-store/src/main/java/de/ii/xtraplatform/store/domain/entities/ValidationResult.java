/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain.entities;

import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
public interface ValidationResult {

  enum MODE {
    NONE,
    LAX,
    STRICT
  }

  static ValidationResult of() {
    return ImmutableValidationResult.builder().mode(MODE.NONE).build();
  }

  default ValidationResult mergeWith(ValidationResult other) {
    return ImmutableValidationResult.builder()
        .from(this)
        .mode(other.getMode())
        .addAllErrors(other.getErrors())
        .addAllStrictErrors(other.getStrictErrors())
        .addAllWarnings(other.getWarnings())
        .build();
  }

  MODE getMode();

  List<String> getErrors();

  List<String> getStrictErrors();

  List<String> getWarnings();

  @Value.Derived
  default boolean isSuccess() {
    return getErrors().isEmpty() && (getMode() == MODE.LAX || getStrictErrors().isEmpty());
  }
}
