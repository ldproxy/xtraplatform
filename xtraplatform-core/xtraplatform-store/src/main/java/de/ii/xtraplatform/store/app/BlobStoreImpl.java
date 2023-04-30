/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.Lists;
import dagger.Lazy;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.StoreSource;
import de.ii.xtraplatform.base.domain.StoreSource.Content;
import de.ii.xtraplatform.store.domain.BlobReader;
import de.ii.xtraplatform.store.domain.BlobSource;
import de.ii.xtraplatform.store.domain.BlobStore;
import de.ii.xtraplatform.store.domain.BlobStoreDriver;
import de.ii.xtraplatform.store.domain.Store;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class BlobStoreImpl implements BlobStore, AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(BlobStoreImpl.class);

  private final Store store;
  private final Lazy<Set<BlobStoreDriver>> drivers;

  private final List<BlobSource> blobReaders;
  private final List<BlobSource> blobWriters;
  private final boolean isReadOnly;

  @Inject
  BlobStoreImpl(Store store, Lazy<Set<BlobStoreDriver>> drivers) {
    this.store = store;
    this.drivers = drivers;
    this.blobReaders = new ArrayList<>();
    this.blobWriters = new ArrayList<>();
    this.isReadOnly = !store.isWritable();
  }

  @Override
  public int getPriority() {
    return 50;
  }

  @Override
  public void onStart() {
    List<StoreSource> sources = findSources();

    Lists.reverse(sources)
        .forEach(
            source -> {
              Optional<BlobStoreDriver> blobStoreDriver = findDriver(source, true);

              blobStoreDriver.ifPresent(
                  driver -> {
                    try {
                      BlobSource blobSource = driver.init(source);

                      blobReaders.add(blobSource);

                      boolean writable = false;
                      if (source.isWritable() && blobSource.canWrite()) {
                        blobWriters.add(blobSource);
                        writable = true;
                      }

                      if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(
                            "{}{} for {} ready{}",
                            Content.RESOURCES.getLabel(),
                            source.getPrefix().isPresent()
                                ? " of type " + source.getPrefix().get()
                                : "",
                            source.getLabel(),
                            writable ? " and writable" : "");
                      }
                    } catch (Throwable e) {
                      LogContext.error(
                          LOGGER,
                          e,
                          "{} for {} could not be loaded",
                          Content.RESOURCES.getLabel(),
                          source.getLabel());
                    }
                  });
            });
  }

  private List<StoreSource> findSources() {
    return store.get().stream()
        .filter(
            source ->
                source.getContent() == Content.ALL || source.getContent() == Content.RESOURCES)
        .collect(Collectors.toUnmodifiableList());
  }

  private Optional<BlobStoreDriver> findDriver(StoreSource storeSource, boolean warn) {
    final boolean[] foundUnavailable = {false};

    // TODO: driver content types, s3 only supports resources
    Optional<BlobStoreDriver> driver =
        drivers.get().stream()
            .filter(d -> d.getType() == storeSource.getType())
            .filter(
                d -> {
                  if (!d.isAvailable(storeSource)) {
                    if (warn) {
                      LOGGER.warn(
                          "{} for {} are not available.",
                          Content.RESOURCES.getLabel(),
                          storeSource.getLabel());
                    }
                    foundUnavailable[0] = true;
                    return false;
                  }
                  return true;
                })
            .findFirst();

    if (driver.isEmpty() && !foundUnavailable[0]) {
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
  public Optional<InputStream> get(Path path) throws IOException {

    for (BlobReader source : blobReaders) {
      Optional<InputStream> blob = source.get(path);

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
    for (BlobReader source : blobReaders) {
      if (source.has(path)) {
        return source.walk(path, maxDepth, matcher);
      }
    }

    return Stream.empty();
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
        "Cannot write {} at '{}', no writable source found.", Content.RESOURCES.getPrefix(), path);
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
        "Cannot delete {} at '{}', no writable source found.", Content.RESOURCES.getPrefix(), path);
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

  @Override
  public Path getPrefix() {
    return Path.of("");
  }
}
