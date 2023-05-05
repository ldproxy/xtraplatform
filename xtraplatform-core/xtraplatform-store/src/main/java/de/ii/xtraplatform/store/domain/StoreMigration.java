/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain;

import de.ii.xtraplatform.base.domain.StoreSourceFs;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.store.domain.StoreMigration.StoreMigrationContext;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface StoreMigration extends Migration<StoreMigrationContext> {
  enum Type {
    BLOB,
    EVENT
  }

  interface StoreMigrationContext extends MigrationContext {
    BlobReader asReader(StoreSourceFs source);
  }

  Type getType();

  List<Tuple<StoreSourceFs, StoreSourceFs>> getMoves();

  List<StoreSourceFs> getCleanups();

  @Override
  default boolean isApplicable(StoreMigrationContext context) {
    return getMoves().stream()
        .anyMatch(
            move -> {
              try {
                return context.asReader(move.first()).has(Path.of(""));
              } catch (IOException e) {
                return false;
              }
            });
  }
}
