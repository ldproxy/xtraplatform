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
import de.ii.xtraplatform.store.domain.BlobLocals;
import de.ii.xtraplatform.store.domain.BlobReader;
import de.ii.xtraplatform.store.domain.BlobSource;
import de.ii.xtraplatform.store.domain.BlobStore;
import de.ii.xtraplatform.store.domain.BlobStoreDriver;
import de.ii.xtraplatform.store.domain.BlobWriter;
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
public class BlobStoreImpl implements BlobStore, BlobLocals, AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(BlobStoreImpl.class);

  private final Store store;
  private final Lazy<Set<BlobStoreDriver>> drivers;

  private final List<BlobSource> blobReaders;
  private Optional<BlobWriter> blobWriter;
  private final boolean isReadOnly;

  @Inject
  BlobStoreImpl(Store store, Lazy<Set<BlobStoreDriver>> drivers) {
    this.store = store;
    this.drivers = drivers;
    this.blobReaders = new ArrayList<>();
    this.blobWriter = Optional.empty();
    this.isReadOnly = !store.isWritable();
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
                      if (blobWriter.isEmpty() && source.isWritable() && blobSource.canWrite()) {
                        this.blobWriter = Optional.of(blobSource.writer());
                        writable = true;
                      }

                      if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(
                            "{} for {} ready{}",
                            Content.RESOURCES.getLabel(),
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
    if (blobWriter.isEmpty()) {
      LOGGER.error(
          "Cannot write {} at '{}', no writable source found.",
          Content.RESOURCES.getPrefix(),
          path);
      return;
    }

    blobWriter.get().put(path, content);
  }

  @Override
  public void delete(Path path) throws IOException {
    if (isReadOnly) {
      LOGGER.error("Store is operating in read-only mode, write operations are not allowed.");
      return;
    }
    if (blobWriter.isEmpty()) {
      LOGGER.error(
          "Cannot delete {} at '{}', no writable source found.",
          Content.RESOURCES.getPrefix(),
          path);
      return;
    }

    blobWriter.get().delete(path);
  }

  @Override
  public Optional<Path> path(Path path) throws IOException {

    for (BlobSource source : blobReaders) {
      if (source.has(path) && source.canLocals()) {
        return source.local().path(path);
      }
    }

    return Optional.empty();
  }
}
