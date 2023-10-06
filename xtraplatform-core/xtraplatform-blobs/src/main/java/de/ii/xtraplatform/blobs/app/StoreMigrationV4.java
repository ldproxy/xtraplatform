/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.blobs.app;

import de.ii.xtraplatform.base.domain.ImmutableStoreSourceDefault;
import de.ii.xtraplatform.base.domain.ImmutableStoreSourceFsV3;
import de.ii.xtraplatform.base.domain.StoreSource;
import de.ii.xtraplatform.base.domain.StoreSourceFs;
import de.ii.xtraplatform.base.domain.StoreSourceFsV3;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.blobs.domain.StoreMigration;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class StoreMigrationV4 implements StoreMigration {

  private final StoreSourceFs source;
  private final StoreMigrationContext context;

  public StoreMigrationV4(StoreSourceFs source, StoreMigrationContext context) {
    this.source = source;
    this.context = context;
  }

  @Override
  public String getSubject() {
    return source.getSrc();
  }

  @Override
  public String getDescription() {
    return "the directory layout is deprecated and will stop working in v4, it will be migrated to the new layout";
  }

  @Override
  public boolean isApplicable(StoreSourceFs ignore) {
    return source instanceof StoreSourceFsV3 && StoreMigration.super.isApplicable(source);
  }

  @Override
  public StoreMigrationContext getContext() {
    return context;
  }

  @Override
  public Type getType() {
    return Type.BLOB;
  }

  @Override
  public List<Tuple<StoreSourceFs, StoreSourceFs>> getMoves() {
    List<StoreSource> froms = new ImmutableStoreSourceFsV3.Builder().build().explode();
    StoreSourceFs to = new ImmutableStoreSourceDefault.Builder().build();

    return froms.stream()
        .map(from -> Tuple.of((StoreSourceFs) from, to))
        .collect(Collectors.toList());
  }

  @Override
  public List<Tuple<Path, Boolean>> getCleanups() {
    return List.of(
        Tuple.of(Path.of("store/resources"), false),
        Tuple.of(Path.of("store"), false),
        Tuple.of(Path.of("api-resources"), false),
        Tuple.of(Path.of("cache/tiles/__tmp__"), true),
        Tuple.of(Path.of("cache/tiles"), false),
        Tuple.of(Path.of("cache/tiles3d/__tmp__"), true),
        Tuple.of(Path.of("cache/tiles3d"), false),
        Tuple.of(Path.of("cache"), false),
        Tuple.of(Path.of("templates"), false));
  }
}
