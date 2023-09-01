/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import static de.ii.xtraplatform.base.domain.StoreSourceFsV3.V3_SOURCES;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableStoreSourceHttpV3.Builder.class)
public interface StoreSourceHttpV3 extends StoreSourceHttp {

  String KEY = "HTTP_V3";

  @JsonProperty(StoreSource.TYPE_PROP)
  @Value.Derived
  default String getTypeString() {
    return Type.HTTP.name();
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
    return V3_SOURCES.stream()
        .map(
            storeSource ->
                new ImmutableStoreSourcePartial.Builder()
                    .from(storeSource)
                    .src("")
                    .archiveRoot("/" + storeSource.getSrc())
                    .build())
        .collect(Collectors.toList());
  }
}
