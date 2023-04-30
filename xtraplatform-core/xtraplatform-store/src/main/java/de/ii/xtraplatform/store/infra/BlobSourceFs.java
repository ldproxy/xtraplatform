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
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlobSourceFs implements BlobSource, BlobWriter, BlobLocals {

  private static final Logger LOGGER = LoggerFactory.getLogger(BlobSourceFs.class);

  private final Path root;
  @Nullable private final Path prefix;

  public BlobSourceFs(Path root) {
    this(root, null);
  }

  public BlobSourceFs(Path root, Path prefix) {
    this.root = root;
    this.prefix = prefix;
  }

  @Override
  public boolean has(Path path) throws IOException {
    if (!canHandle(path)) {
      return false;
    }

    return Files.exists(full(path));
  }

  @Override
  public Optional<InputStream> get(Path path) throws IOException {
    if (!canHandle(path)) {
      return Optional.empty();
    }

    Path filePath = full(path);

    if (!Files.exists(filePath)) {
      return Optional.empty();
    }

    return Optional.of(Files.newInputStream(filePath));
  }

  @Override
  public long size(Path path) throws IOException {
    if (!canHandle(path)) {
      return -1;
    }

    Path filePath = full(path);

    if (!Files.isRegularFile(filePath)) {
      return -1;
    }

    return Files.size(full(path));
  }

  @Override
  public long lastModified(Path path) throws IOException {
    if (!canHandle(path)) {
      return -1;
    }

    Path filePath = full(path);

    if (!Files.isRegularFile(filePath)) {
      return -1;
    }

    return Files.getLastModifiedTime(full(path)).toMillis();
  }

  @Override
  public Stream<Path> walk(Path path, int maxDepth, BiPredicate<Path, PathAttributes> matcher)
      throws IOException {
    if (!canHandle(path)) {
      return Stream.empty();
    }

    Path dir = full(path);
    return Files.find(
            dir,
            maxDepth,
            ((path1, basicFileAttributes) ->
                matcher.test(
                    dir.relativize(path1),
                    new PathAttributes() {
                      @Override
                      public boolean isValue() {
                        return basicFileAttributes.isRegularFile();
                      }

                      @Override
                      public boolean isHidden() {
                        return path1.getFileName().toString().startsWith(".");
                      }
                    })))
        .map(dir::relativize);
  }

  @Override
  public void put(Path path, InputStream content) throws IOException {
    if (!canHandle(path)) {
      return;
    }

    Path filePath = full(path);

    if (!Files.exists(filePath) || Files.isWritable(filePath)) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Writing blob at {}", filePath);
      }

      Files.createDirectories(filePath.getParent());

      try (OutputStream file = Files.newOutputStream(filePath)) {
        content.transferTo(file);
      }
    }
  }

  @Override
  public void delete(Path path) throws IOException {
    if (!canHandle(path)) {
      return;
    }

    Path filePath = full(path);

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Deleting blob at {}", filePath);
    }

    Files.delete(filePath);
  }

  // TODO: remote sources might provide readable locals, but never writable ones
  @Override
  public Optional<Path> asLocalPath(Path path, boolean writable) throws IOException {
    if (!canHandle(path)) {
      return Optional.empty();
    }

    if (writable || has(path)) {
      Path filePath = full(path);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Providing writable local blob at {}", filePath);
      }

      return Optional.of(filePath);
    }

    return Optional.empty();
  }

  private Path full(Path path) {
    return Objects.isNull(prefix) ? root.resolve(path) : root.resolve(prefix.relativize(path));
  }

  @Override
  public boolean canHandle(Path path) {
    return Objects.isNull(prefix) || path.startsWith(prefix);
  }
}
