/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public interface BlobReader {

  boolean has(Path path) throws IOException;

  Optional<InputStream> get(Path path) throws IOException;

  long size(Path path) throws IOException;

  long lastModified(Path path) throws IOException;

  Stream<Path> walk(Path path, int maxDepth, BiPredicate<Path, PathAttributes> matcher)
      throws IOException;

  interface PathAttributes {
    boolean isValue();

    boolean isHidden();
  }
}
