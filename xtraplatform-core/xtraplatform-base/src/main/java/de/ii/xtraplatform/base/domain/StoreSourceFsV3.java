/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import static de.ii.xtraplatform.base.domain.StoreConfiguration.OLD_DEFAULT_LOCATION;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableStoreSourceFsV3.Builder.class)
public interface StoreSourceFsV3 extends StoreSourceFs {

  String KEY = "FS_V3";

  static boolean isOldDefaultStore(StoreSource storeSource) {
    return storeSource instanceof StoreSourceFs
        && storeSource.getContent() == Content.ENTITIES
        && (Objects.equals(storeSource.getSrc(), OLD_DEFAULT_LOCATION)
            || Objects.equals(storeSource.getSrc(), "./" + OLD_DEFAULT_LOCATION));
  }

  List<StoreSource> V3_SOURCES =
      List.of(
          new ImmutableStoreSourceFs.Builder()
              .type(Type.FS_KEY)
              .content(Content.CFG)
              .src("cfg.yml")
              .desiredMode(Mode.RO)
              .build(),
          new ImmutableStoreSourceFs.Builder()
              .type(Type.FS_KEY)
              .content(Content.ENTITIES)
              .src(OLD_DEFAULT_LOCATION)
              .addExcludes("entities/codelists/**")
              .build(),
          new ImmutableStoreSourceFs.Builder()
              .type(Type.FS_KEY)
              .content(Content.VALUES)
              .src(OLD_DEFAULT_LOCATION + "/entities/codelists")
              .prefix("codelists")
              .build(),
          new ImmutableStoreSourceFs.Builder()
              .type(Type.FS_KEY)
              .content(Content.VALUES)
              .src("api-resources/tile-matrix-sets")
              .prefix("tile-matrix-sets")
              .build(),
          new ImmutableStoreSourceFs.Builder()
              .type(Type.FS_KEY)
              .content(Content.VALUES)
              .src("api-resources/queries")
              .prefix("queries")
              .build(),
          new ImmutableStoreSourceFs.Builder()
              .type(Type.FS_KEY)
              .content(Content.VALUES)
              .src("api-resources/styles")
              .prefix("maplibre-styles")
              .addIncludes("**/*.mbs")
              .build(),
          new ImmutableStoreSourceFs.Builder()
              .type(Type.FS_KEY)
              .content(Content.VALUES)
              .src("api-resources/routes")
              .prefix("routes/results")
              .addExcludes("**/*.definition.json")
              .build(),
          new ImmutableStoreSourceFs.Builder()
              .type(Type.FS_KEY)
              .content(Content.VALUES)
              .src("api-resources/routes")
              .prefix("routes/definitions")
              .addIncludes("**/*.definition.json")
              .build(),
          new ImmutableStoreSourceFs.Builder()
              .type(Type.FS_KEY)
              .content(Content.RESOURCES)
              .src("api-resources")
              .build(),
          new ImmutableStoreSourceFs.Builder()
              .type(Type.FS_KEY)
              .content(Content.RESOURCES)
              .src("api-resources/resources")
              .prefix("api-resources")
              .build(),
          new ImmutableStoreSourceFs.Builder()
              .type(Type.FS_KEY)
              .content(Content.RESOURCES)
              .src("api-resources/styles")
              .prefix("other-styles")
              .addExcludes("**/*.mbs")
              .build(),
          new ImmutableStoreSourceFs.Builder()
              .type(Type.FS_KEY)
              .content(Content.RESOURCES)
              .src(OLD_DEFAULT_LOCATION + "/resources")
              .desiredMode(Mode.RO)
              .build(),
          new ImmutableStoreSourceFs.Builder()
              .type(Type.FS_KEY)
              .content(Content.RESOURCES)
              .src("cache/tiles")
              .prefix("tiles")
              .build(),
          new ImmutableStoreSourceFs.Builder()
              .type(Type.FS_KEY)
              .content(Content.RESOURCES)
              .src("cache/tiles3d")
              .prefix("tiles3d")
              .build(),
          new ImmutableStoreSourceFs.Builder()
              .type(Type.FS_KEY)
              .content(Content.RESOURCES)
              .src("proj")
              .prefix("proj")
              .build(),
          new ImmutableStoreSourceFs.Builder()
              .type(Type.FS_KEY)
              .content(Content.RESOURCES)
              .src("templates/html")
              .prefix("html/templates")
              .desiredMode(Mode.RO)
              .build());

  List<StoreSourcePartial> V3_PARTIALS =
      V3_SOURCES.stream()
          .map(storeSource -> new ImmutableStoreSourcePartial.Builder().from(storeSource).build())
          .collect(Collectors.toList());

  @JsonProperty(StoreSource.TYPE_PROP)
  @Value.Derived
  default String getType() {
    return Type.FS.name();
  }

  @Value.Derived
  @Override
  default Content getContent() {
    return Content.MULTI;
  }

  @Value.Default
  @Override
  default String getSrc() {
    return ".";
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
