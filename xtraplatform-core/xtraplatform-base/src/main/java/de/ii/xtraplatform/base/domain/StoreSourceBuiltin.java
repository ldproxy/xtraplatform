/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import static de.ii.xtraplatform.base.domain.StoreConfiguration.DEFAULT_LOCATION;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableStoreSourceBuiltin.Builder.class)
public interface StoreSourceBuiltin extends StoreSourceFs {

  String KEY = "FS_DEFAULT";

  @JsonIgnore
  @Value.Derived
  default Type getType() {
    return Type.FS;
  }

  @JsonIgnore
  @Value.Derived
  @Override
  default Content getContent() {
    return Content.ALL;
  }

  @JsonIgnore
  @Value.Derived
  @Override
  default String getSrc() {
    return DEFAULT_LOCATION;
  }
}
