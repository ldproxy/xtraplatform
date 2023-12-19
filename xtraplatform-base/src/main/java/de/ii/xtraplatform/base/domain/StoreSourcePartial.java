/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableStoreSourcePartial.Builder.class)
public interface StoreSourcePartial extends StoreSource {

  @Value.Derived
  @Override
  default String getType() {
    return Type.EMPTY_KEY;
  }

  @Value.Default
  @Override
  default String getSrc() {
    return "";
  }

  @Value.Default
  @Override
  default boolean isWatchable() {
    return false;
  }
}
