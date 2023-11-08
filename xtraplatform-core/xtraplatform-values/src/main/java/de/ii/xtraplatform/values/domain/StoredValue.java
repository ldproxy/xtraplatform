/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface StoredValue {

  // TODO: removing/emptying breaks builders, no from(Value) is generated
  @JsonIgnore
  @org.immutables.value.Value.Default
  default long storageVersion() {
    return 1L;
  }
}
