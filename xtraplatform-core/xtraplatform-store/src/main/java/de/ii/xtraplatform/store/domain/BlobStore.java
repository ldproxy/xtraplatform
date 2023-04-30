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

public interface BlobStore extends BlobReader, BlobWriter, BlobLocals {

  Path getPrefix();

  default BlobStore with(String type, String... path) {
    BlobStore delegate = this;
    Path prefix = Path.of(type, path);

    return new BlobStore() {
      @Override
      public Path getPrefix() {
        return prefix;
      }

      @Override
      public boolean has(Path path) throws IOException {
        return delegate.has(prefix.resolve(path));
      }

      @Override
      public Optional<InputStream> get(Path path) throws IOException {
        return delegate.get(prefix.resolve(path));
      }

      @Override
      public long size(Path path) throws IOException {
        return delegate.size(prefix.resolve(path));
      }

      @Override
      public long lastModified(Path path) throws IOException {
        return delegate.lastModified(prefix.resolve(path));
      }

      @Override
      public Stream<Path> walk(Path path, int maxDepth, BiPredicate<Path, PathAttributes> matcher)
          throws IOException {
        return delegate.walk(prefix.resolve(path), maxDepth, matcher);
      }

      @Override
      public void put(Path path, InputStream content) throws IOException {
        delegate.put(prefix.resolve(path), content);
      }

      @Override
      public void delete(Path path) throws IOException {
        delegate.delete(prefix.resolve(path));
      }

      @Override
      public Optional<Path> asLocalPath(Path path, boolean writable) throws IOException {
        return delegate.asLocalPath(prefix.resolve(path), writable);
      }
    };
  }
}
