/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableStoreSourceCache3dV3.Builder.class)
public interface StoreSourceCache3dV3 extends StoreSourceFs {

  String KEY = "FS_CACHE_3D_V3";

  @JsonProperty(StoreSource.TYPE_PROP)
  @Value.Derived
  default String getTypeString() {
    return Type.FS.name();
  }

  @Value.Derived
  @Override
  default Content getContent() {
    return Content.RESOURCES;
  }

  @Value.Derived
  @Override
  default String getSrc() {
    return "cache/tiles3d";
  }

  @Value.Derived
  @Override
  default Optional<String> getPrefix() {
    return Optional.of("tiles3d");
  }
}
