/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.infra;

import de.ii.xtraplatform.store.domain.BlobLocals;
import de.ii.xtraplatform.store.domain.BlobSource;
import de.ii.xtraplatform.store.domain.BlobWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public class BlobSourceFs implements BlobSource, BlobWriter, BlobLocals {

  private final Path root;

  public BlobSourceFs(Path root) {
    this.root = root;
  }

  @Override
  public boolean has(Path path) throws IOException {
    return Files.exists(full(path));
  }

  @Override
  public Optional<InputStream> get(Path path) throws IOException {
    Path filePath = full(path);

    if (Files.notExists(filePath)) {
      return Optional.empty();
    }

    return Optional.of(Files.newInputStream(filePath));
  }

  @Override
  public long size(Path path) throws IOException {
    Path filePath = full(path);

    if (!Files.isRegularFile(filePath)) {
      return -1;
    }

    return Files.size(full(path));
  }

  @Override
  public Stream<Path> walk(Path path, int maxDepth, BiPredicate<Path, PathAttributes> matcher)
      throws IOException {
    Path dir = root.resolve(path);
    return Files.find(
        dir,
        maxDepth,
        ((path1, basicFileAttributes) ->
            matcher.test(dir.relativize(path1), basicFileAttributes::isRegularFile)));
  }

  @Override
  public void put(Path path, InputStream content) throws IOException {
    Path filePath = full(path);

    if (Files.notExists(filePath) || Files.isWritable(filePath)) {
      Files.createDirectories(filePath.getParent());

      try (OutputStream file = Files.newOutputStream(filePath)) {
        content.transferTo(file);
      }
    }
  }

  @Override
  public void delete(Path path) throws IOException {
    Files.delete(full(path));
  }

  @Override
  public Optional<Path> path(Path path) throws IOException {
    if (has(path)) {
      return Optional.of(full(path));
    }

    return Optional.empty();
  }

  private Path full(Path path) {
    return root.resolve(path);
  }
}
