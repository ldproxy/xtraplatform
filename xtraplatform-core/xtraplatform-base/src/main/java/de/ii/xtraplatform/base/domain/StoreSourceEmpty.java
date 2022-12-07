/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableStoreSourceEmpty.Builder.class)
public interface StoreSourceEmpty extends StoreSource {

  @JsonIgnore
  @Value.Derived
  @Override
  default Content getContent() {
    return Content.NONE;
  }

  @JsonIgnore
  @Value.Derived
  @Override
  default String getSrc() {
    return "";
  }

  @JsonIgnore
  @Override
  default Mode getDesiredMode() {
    return Mode.RO;
  }

  @JsonIgnore
  @Override
  Optional<String> getPrefix();

  @JsonIgnore
  @Override
  default String getArchiveRoot() {
    return StoreSource.super.getArchiveRoot();
  }

  @JsonIgnore
  @Override
  default boolean getArchiveCache() {
    return StoreSource.super.getArchiveCache();
  }

  @JsonIgnore
  @Value.Derived
  @Override
  default boolean isWatchable() {
    return false;
  }
}
