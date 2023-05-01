/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import static de.ii.xtraplatform.base.domain.StoreConfiguration.DEFAULT_LOCATION;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableStoreSourceDefaultV3.Builder.class)
public interface StoreSourceDefaultV3 extends StoreSourceFs {

  String KEY = "FS_STORE_V3";

  @JsonProperty(StoreSource.TYPE_PROP)
  @Value.Derived
  default String getTypeString() {
    return Type.FS.name();
  }

  @Value.Derived
  @Override
  default Content getContent() {
    return Content.ALL;
  }

  @Value.Derived
  @Override
  default String getSrc() {
    return DEFAULT_LOCATION;
  }
}
