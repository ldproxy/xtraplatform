/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableStoreSourceProjV3.Builder.class)
public interface StoreSourceProjV3 extends StoreSourceFs {

  String KEY = "FS_PROJ_V3";

  @JsonProperty(StoreSource.TYPE_PROP)
  @Value.Default
  default String getTypeString() {
    return KEY;
  }

  @JsonIgnore
  @Value.Derived
  @Override
  default Type getType() {
    return Type.FS;
  }

  @JsonIgnore
  @Value.Derived
  @Override
  default Content getContent() {
    return Content.RESOURCES;
  }

  @JsonIgnore
  @Value.Derived
  @Override
  default String getSrc() {
    return "proj";
  }

  @JsonIgnore
  @Value.Derived
  @Override
  default Optional<String> getPrefix() {
    return Optional.of("proj");
  }
}
