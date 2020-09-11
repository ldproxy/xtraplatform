/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.Optional;
import org.immutables.value.Value;

/** @author zahnen */
public interface EntityData extends de.ii.xtraplatform.store.domain.Value {

  String getId();

  @Value.Default
  default long getCreatedAt() {
    return Instant.now().toEpochMilli();
  }

  @Value.Default
  default long getLastModified() {
    return Instant.now().toEpochMilli();
  }

  @Value.Default
  default long getEntityStorageVersion() {
    return 1;
  }

  @JsonIgnore
  @Value.Derived
  default long getEntitySchemaVersion() {
    return 1;
  }

  @JsonIgnore
  Optional<String> getEntitySubType();
}
