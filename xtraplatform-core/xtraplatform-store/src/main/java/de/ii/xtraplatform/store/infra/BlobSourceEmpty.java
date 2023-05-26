/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.infra;

import de.ii.xtraplatform.store.domain.BlobSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public class BlobSourceEmpty implements BlobSource {

  @Override
  public boolean has(Path path) throws IOException {
    return false;
  }

  @Override
  public Optional<InputStream> get(Path path) throws IOException {
    return Optional.empty();
  }

  @Override
  public long size(Path path) throws IOException {
    return 0;
  }

  @Override
  public long lastModified(Path path) throws IOException {
    return 0;
  }

  @Override
  public Stream<Path> walk(Path path, int maxDepth, BiPredicate<Path, PathAttributes> matcher)
      throws IOException {
    return Stream.empty();
  }

  @Override
  public boolean canHandle(Path path) {
    return false;
  }
}
