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
  default boolean isWatchable() {
    return !isArchive();
  }

  default Path getAbsolutePath(Path root) {
    Path src = Path.of(getSrc());
    return src.isAbsolute() ? src : root.resolve(src);
  }
}
