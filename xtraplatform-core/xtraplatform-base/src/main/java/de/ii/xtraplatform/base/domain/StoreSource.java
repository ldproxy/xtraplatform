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
import java.nio.file.Path;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableStoreSource.Builder.class)
public interface StoreSource {

  enum Type {
    FS,
    ZIP,
    GIT,
    REF
  }

  enum Content {
    ALL,
    DEFAULTS,
    ENTITIES,
    OVERRIDES,
    BLOBS,
    LOCALS
  }

  enum Mode {
    RO,
    RW,
  }

  Type getType();

  @Value.Default
  default Content getContent() {
    return Content.ALL;
  }

  @Value.Default
  default Mode getMode() {
    return getType() == Type.FS ? Mode.RW : Mode.RO;
  }

  @Value.Default
  default boolean isWatchable() {
    return getType() == Type.FS;
  }

  String getSrc();

  Optional<String> getId();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default String getShortLabel() {
    return String.format("%s(%s)", getType(), getSrc());
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default String getLabel() {
    return String.format("%s %s [%s] [%s]", getType(), getSrc(), getContent(), getMode());
  }

  default String getLabel(Path rootDir) {
    Path src = Path.of(getSrc());
    return String.format(
        "%s %s [%s] [%s]",
        getType(), src.isAbsolute() ? src : rootDir.resolve(src), getContent(), getMode());
  }
}
