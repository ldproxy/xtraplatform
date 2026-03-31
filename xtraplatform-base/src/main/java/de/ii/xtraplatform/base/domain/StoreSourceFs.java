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
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableStoreSourceFs.Builder.class)
public interface StoreSourceFs extends StoreSource {

  @JsonProperty(StoreSource.MODE_PROP)
  @Value.Default
  @Override
  default Mode getDesiredMode() {
    return Mode.RW;
  }

  @Value.Default
  @Override
  default boolean isWatchable() {
    return !isArchive();
  }

  @Value.Default
  default boolean isCreate() {
    return !isArchive() && !Path.of(getSrc()).isAbsolute();
  }

  default Path getAbsolutePath(Path root) {
    Path src = Path.of(getSrc());
    return src.isAbsolute() ? src : root.resolve(src);
  }

  @Override
  default List<StoreSource> explode() {
    if (getContent() == Content.MULTI) {
      return getParts().stream()
          .map(
              part ->
                  new ImmutableStoreSourceFs.Builder()
                      .from(this)
                      .from(part)
                      .type(getType())
                      .src(
                          getSrc()
                              + (getSrc().endsWith("/") || part.getSrc().startsWith("/") ? "" : "/")
                              + part.getSrc())
                      .parts(List.of())
                      .build())
          .collect(Collectors.toList());
    }

    return StoreSource.super.explode();
  }
}
