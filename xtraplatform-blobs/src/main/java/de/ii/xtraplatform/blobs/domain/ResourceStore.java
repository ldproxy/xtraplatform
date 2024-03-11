/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.blobs.domain;

import de.ii.xtraplatform.base.domain.resiliency.Volatile2;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry.ChangeHandler;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public interface ResourceStore extends BlobReader, BlobWriter, BlobLocals, Volatile2 {

  CompletableFuture<Void> onReady();

  Path getPrefix();

  default ResourceStore with(String type, String... path) {
    return writableWith(false, type, path);
  }

  default ResourceStore writableWith(String type, String... path) {
    return writableWith(true, type, path);
  }

  default ResourceStore writableWith(boolean writable, String type, String... path) {
    ResourceStore delegate = this;
    Path prefix = Path.of(type, path);

    if (!(delegate instanceof BlobWriterReader)) {
      throw new IllegalStateException();
    }
    BlobWriterReader delegateWriter = (BlobWriterReader) this;

    return new PrefixedResourceStore(delegate, prefix, delegateWriter, writable);
  }

  class PrefixedResourceStore implements ResourceStore, BlobWriterReader {

    private final ResourceStore delegate;
    private final Path prefix;
    private final BlobWriterReader delegateWriter;
    private final boolean writable;

    public PrefixedResourceStore(
        ResourceStore delegate, Path prefix, BlobWriterReader delegateWriter, boolean writable) {
      this.delegate = delegate;
      this.prefix = prefix;
      this.delegateWriter = delegateWriter;
      this.writable = writable;
    }

    @Override
    public CompletableFuture<Void> onReady() {
      return delegate.onReady();
    }

    @Override
    public Path getPrefix() {
      return prefix;
    }

    @Override
    public boolean has(Path path) throws IOException {
      return delegateWriter.has(prefix.resolve(path), writable);
    }

    @Override
    public Optional<InputStream> content(Path path) throws IOException {
      return delegateWriter.content(prefix.resolve(path), writable);
    }

    @Override
    public Optional<Blob> get(Path path) throws IOException {
      return delegateWriter.get(prefix.resolve(path), writable);
    }

    @Override
    public String getUniqueKey() {
      return String.format("resources/%s", prefix);
    }

    @Override
    public State getState() {
      return delegate.getState();
    }

    @Override
    public Optional<String> getMessage() {
      return delegate.getMessage();
    }

    @Override
    public Runnable onStateChange(ChangeHandler handler, boolean initialCall) {
      return delegate.onStateChange(handler, initialCall);
    }

    @Override
    public long size(Path path) throws IOException {
      return delegateWriter.size(prefix.resolve(path), writable);
    }

    @Override
    public long lastModified(Path path) throws IOException {
      return delegateWriter.lastModified(prefix.resolve(path), writable);
    }

    @Override
    public Stream<Path> walk(Path path, int maxDepth, BiPredicate<Path, PathAttributes> matcher)
        throws IOException {
      return delegateWriter.walk(prefix.resolve(path), maxDepth, matcher, writable);
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

    @Override
    public boolean has(Path path, boolean writable) throws IOException {
      return delegateWriter.has(prefix.resolve(path), writable);
    }

    @Override
    public Optional<InputStream> content(Path path, boolean writable) throws IOException {
      return delegateWriter.content(prefix.resolve(path), writable);
    }

    @Override
    public Optional<Blob> get(Path path, boolean writable) throws IOException {
      return delegateWriter.get(prefix.resolve(path), writable);
    }

    @Override
    public long size(Path path, boolean writable) throws IOException {
      return delegateWriter.size(prefix.resolve(path), writable);
    }

    @Override
    public long lastModified(Path path, boolean writable) throws IOException {
      return delegateWriter.lastModified(prefix.resolve(path), writable);
    }

    @Override
    public Stream<Path> walk(
        Path path, int maxDepth, BiPredicate<Path, PathAttributes> matcher, boolean writable)
        throws IOException {
      return delegateWriter.walk(prefix.resolve(path), maxDepth, matcher, writable);
    }
  }
}
