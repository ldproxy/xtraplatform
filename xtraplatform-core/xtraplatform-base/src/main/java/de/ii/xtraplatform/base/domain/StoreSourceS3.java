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
import java.util.List;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableStoreSourceS3.Builder.class)
public interface StoreSourceS3 extends StoreSource {

  String getAccessKey();

  String getSecretKey();

  @JsonIgnore
  @Value.Derived
  @Override
  default Mode getDesiredMode() {
    return Mode.RW;
  }

  @JsonIgnore
  @Value.Derived
  default boolean isWatchable() {
    return false;
  }

  @JsonIgnore
  @Value.Derived
  default boolean isCreate() {
    return false;
  }

  @Override
  default List<StoreSource> explode() {
    if (getContent() == Content.MULTI) {
      return getParts().stream()
          .map(
              part ->
                  new ImmutableStoreSourceS3.Builder()
                      .from(this)
                      .from(part)
                      .type(this.getType())
                      .src(this.getSrc())
                      .archiveRoot(
                          getArchiveRoot()
                              + (getArchiveRoot().endsWith("/")
                                      || part.getArchiveRoot().startsWith("/")
                                  ? ""
                                  : "/")
                              + part.getArchiveRoot())
                      .label(this.getLabel())
                      .parts(List.of())
                      .build())
          .collect(Collectors.toList());
    }

    return StoreSource.super.explode();
  }
}
