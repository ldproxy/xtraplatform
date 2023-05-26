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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableStoreSourceFsV3.Builder.class)
public interface StoreSourceFsV3 extends StoreSourceFs {

  String KEY = "FS_V3";

  List<StoreSource> V3_SOURCES =
      List.of(
          new ImmutableStoreSourceCfgV3.Builder().build(),
          new ImmutableStoreSourceDefaultV3.Builder().build(),
          new ImmutableStoreSourceApiResourcesV3.Builder().build(),
          new ImmutableStoreSourceApiResourcesResourcesV3.Builder().build(),
          new ImmutableStoreSourceCacheV3.Builder().build(),
          new ImmutableStoreSourceCache3dV3.Builder().build(),
          new ImmutableStoreSourceProjV3.Builder().build(),
          new ImmutableStoreSourceTemplatesV3.Builder().build());

  List<StoreSourcePartial> V3_PARTIALS =
      V3_SOURCES.stream()
          .map(storeSource -> new ImmutableStoreSourcePartial.Builder().from(storeSource).build())
          .collect(Collectors.toList());

  @JsonProperty(StoreSource.TYPE_PROP)
  @Value.Derived
  default String getTypeString() {
    return Type.FS.name();
  }

  @Value.Derived
  @Override
  default Content getContent() {
    return Content.MULTI;
  }

  @Value.Derived
  @Override
  default Optional<String> getPrefix() {
    return Optional.empty();
  }

  @Value.Derived
  @Override
  default List<StoreSourcePartial> getParts() {
    return V3_PARTIALS;
  }
}
