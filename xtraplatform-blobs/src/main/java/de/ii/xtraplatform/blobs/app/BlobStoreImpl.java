/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.blobs.app;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.mayThrow;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.Lists;
import dagger.Lazy;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.Store;
import de.ii.xtraplatform.base.domain.StoreSource;
import de.ii.xtraplatform.base.domain.StoreSource.Content;
import de.ii.xtraplatform.base.domain.resiliency.AbstractVolatileComposedPolling;
import de.ii.xtraplatform.base.domain.resiliency.AbstractVolatilePolling;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.blobs.domain.Blob;
import de.ii.xtraplatform.blobs.domain.BlobReader;
import de.ii.xtraplatform.blobs.domain.BlobSource;
import de.ii.xtraplatform.blobs.domain.BlobStore;
import de.ii.xtraplatform.blobs.domain.BlobStoreDriver;
import de.ii.xtraplatform.blobs.domain.BlobWriterReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.GodClass"})
public class BlobStoreImpl extends AbstractVolatileComposedPolling
    implements BlobStore, BlobWriterReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(BlobStoreImpl.class);

  private final Store store;
  private final VolatileRegistry volatileRegistry;
  private final Lazy<Set<BlobStoreDriver>> drivers;

  private final List<BlobSource> blobReaders;
  private final List<BlobSource> blobWriters;
  private final boolean isReadOnly;
  private final Content contentType;

  public BlobStoreImpl(
      Store store,
      VolatileRegistry volatileRegistry,
      Lazy<Set<BlobStoreDriver>> drivers,
      Content contentType) {
    super(volatileRegistry, "read", "write");
    this.store = store;
    this.volatileRegistry = volatileRegistry;
    this.drivers = drivers;
    this.blobReaders = new ArrayList<>();
    this.blobWriters = new ArrayList<>();
    this.isReadOnly = !store.isWritable();
    this.contentType = contentType;
  }

  @Override
  public String getUniqueKey() {
    return String.format("app/store/%s", contentType.getPrefix());
  }

  @Override
  public int getIntervalMs() {
    return 5000;
  }

  @Override
  public Tuple<State, String> check() {
    return Tuple.of(State.AVAILABLE, null);
  }

  @Override
  public void start() {
    onVolatileStart();

    List<StoreSource> sources = findSources();

    Lists.reverse(sources).forEach(this::initializeSource);

    onVolatileStarted();
  }

  private void initializeSource(StoreSource source) {
    Optional<BlobStoreDriver> blobStoreDriver = findDriver(source, true);

    blobStoreDriver.ifPresent(driver -> processDriver(driver, source));
  }

  private void processDriver(BlobStoreDriver driver, StoreSource source) {
    try {
      addSubComponent(driver, source);

      BlobSource blobSource = driver.init(source, contentType);

      blobReaders.add(blobSource);

      boolean writable = addWriterIfPossible(source, blobSource);

      logSourceReady(source, writable);
    } catch (Throwable e) {
      LogContext.error(
          LOGGER, e, "{} for {} could not be loaded", contentType.getLabel(), source.getLabel());
    }
  }

  private boolean addWriterIfPossible(StoreSource source, BlobSource blobSource) {
    boolean writable = false;
    if (source.isWritable() && blobSource.canWrite()) {
      blobWriters.add(blobSource);
      writable = true;
    }
    return writable;
  }

  private void logSourceReady(StoreSource source, boolean writable) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "{}{} for {} ready{}",
          contentType.getLabel(),
          source.getPrefix().isPresent() ? " of type " + source.getPrefix().get() : "",
          source.getLabel(),
          writable ? " and writable" : "");
    }
  }

  private void addSubComponent(BlobStoreDriver driver, StoreSource source) {
    addSubcomponent(
        new AbstractVolatilePolling(volatileRegistry) {
          @Override
          public String getUniqueKey() {
            return source.getLabel();
          }

          @Override
          public int getIntervalMs() {
            return BlobStoreImpl.this.getIntervalMs();
          }

          @Override
          public Tuple<State, String> check() {
            if (driver.isAvailable(source)) {
              return Tuple.of(State.AVAILABLE, null);
            }
            return Tuple.of(State.UNAVAILABLE, null);
          }

          @Override
          protected Set<String> getVolatileCapabilities() {
            return source.isWritable() ? Set.of("read", "write") : Set.of("read");
          }

          @Override
          public Optional<HealthCheck> asHealthCheck() {
            return Optional.empty();
          }
        });
  }

  private List<StoreSource> findSources() {
    return store.get().stream()
        .filter(source -> source.getContent() == Content.ALL || source.getContent() == contentType)
        .collect(Collectors.toUnmodifiableList());
  }

  private Optional<BlobStoreDriver> findDriver(StoreSource storeSource, boolean warn) {
    final boolean[] foundUnavailable = {false};

    Optional<BlobStoreDriver> driver =
        drivers.get().stream()
            .filter(d -> Objects.equals(d.getType(), storeSource.getType()))
            .filter(
                d -> {
                  if (!d.isAvailable(storeSource)) {
                    if (warn && LOGGER.isWarnEnabled()) {
                      LOGGER.warn(
                          "{} for {} are not available.",
                          contentType.getLabel(),
                          storeSource.getLabel());
                    }
                    foundUnavailable[0] = true;
                    return false;
                  }
                  return true;
                })
            .findFirst();

    if (driver.isEmpty() && !foundUnavailable[0] && LOGGER.isErrorEnabled()) {
      LOGGER.error("No blob driver found for source {}.", storeSource.getLabel());
    }

    return driver;
  }

  @Override
  public boolean has(Path path) throws IOException {

    for (BlobReader source : blobReaders) {
      if (source.has(path)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public Optional<InputStream> content(Path path) throws IOException {

    for (BlobReader source : blobReaders) {
      Optional<InputStream> blob = source.content(path);

      if (blob.isPresent()) {
        return blob;
      }
    }

    return Optional.empty();
  }

  @Override
  public Optional<Blob> get(Path path) throws IOException {
    for (BlobReader source : blobReaders) {
      Optional<Blob> blob = source.get(path);

      if (blob.isPresent()) {
        return blob;
      }
    }

    return Optional.empty();
  }

  @Override
  public long size(Path path) throws IOException {
    for (BlobReader source : blobReaders) {
      long size = source.size(path);

      if (size > -1) {
        return size;
      }
    }

    return -1;
  }

  @Override
  public long lastModified(Path path) throws IOException {
    for (BlobReader source : blobReaders) {
      long size = source.lastModified(path);

      if (size > -1) {
        return size;
      }
    }

    return -1;
  }

  @Override
  public Stream<Path> walk(Path path, int maxDepth, BiPredicate<Path, PathAttributes> matcher)
      throws IOException {
    try {
      return blobReaders.stream()
          .flatMap(mayThrow(reader -> reader.walk(path, maxDepth, matcher)))
          .distinct();
    } catch (Throwable e) {
      throw new IOException("Error in BlobStore.walk", e);
    }
  }

  @Override
  public void put(Path path, InputStream content) throws IOException {
    if (isReadOnly) {
      LOGGER.error("Store is operating in read-only mode, write operations are not allowed.");
      return;
    }

    for (BlobSource source : blobWriters) {
      if (source.canHandle(path)) {
        source.writer().put(path, content);
        return;
      }
    }

    LOGGER.error(
        "Cannot write {} at '{}', no writable source found.", contentType.getPrefix(), path);
  }

  @Override
  public void delete(Path path) throws IOException {
    if (isReadOnly) {
      LOGGER.error("Store is operating in read-only mode, write operations are not allowed.");
      return;
    }

    for (BlobSource source : blobWriters) {
      if (source.canHandle(path)) {
        source.writer().delete(path);
        return;
      }
    }

    LOGGER.error(
        "Cannot delete {} at '{}', no writable source found.", contentType.getPrefix(), path);
  }

  @Override
  public boolean has(Path path, boolean writable) throws IOException {
    if (!writable) {
      return has(path);
    }

    for (BlobSource source : blobWriters) {
      if (source.canHandle(path)) {
        return source.has(path);
      }
    }

    return false;
  }

  @Override
  public Optional<InputStream> content(Path path, boolean writable) throws IOException {
    if (!writable) {
      return content(path);
    }

    for (BlobSource source : blobWriters) {
      if (source.canHandle(path)) {
        return source.content(path);
      }
    }

    return Optional.empty();
  }

  @Override
  public Optional<Blob> get(Path path, boolean writable) throws IOException {
    if (!writable) {
      return get(path);
    }

    for (BlobSource source : blobWriters) {
      if (source.canHandle(path)) {
        return source.get(path);
      }
    }

    return Optional.empty();
  }

  @Override
  public long size(Path path, boolean writable) throws IOException {
    if (!writable) {
      return size(path);
    }

    for (BlobSource source : blobWriters) {
      if (source.canHandle(path)) {
        return source.size(path);
      }
    }

    return -1;
  }

  @Override
  public long lastModified(Path path, boolean writable) throws IOException {
    if (!writable) {
      return lastModified(path);
    }

    for (BlobSource source : blobWriters) {
      if (source.canHandle(path)) {
        return source.lastModified(path);
      }
    }

    return -1;
  }

  @Override
  public Stream<Path> walk(
      Path path, int maxDepth, BiPredicate<Path, PathAttributes> matcher, boolean writable)
      throws IOException {
    if (!writable) {
      return walk(path, maxDepth, matcher);
    }

    for (BlobSource source : blobWriters) {
      if (source.canHandle(path)) {
        return source.walk(path, maxDepth, matcher);
      }
    }

    return Stream.empty();
  }

  @Override
  public Optional<Path> asLocalPath(Path path, boolean writable) throws IOException {

    if (writable) {
      for (BlobSource writer : blobWriters) {
        if (writer.canHandle(path) && writer.canLocals()) {
          return writer.local().asLocalPath(path, true);
        }
      }

      LOGGER.error("Cannot provide local path for '{}', no local writable source found.", path);

      return Optional.empty();
    }

    for (BlobSource source : blobReaders) {
      if (source.has(path) && source.canLocals()) {
        return source.local().asLocalPath(path, false);
      }
    }

    return Optional.empty();
  }
}
