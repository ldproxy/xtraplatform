/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.blobs.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.blobs.domain.BlobCache;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Singleton
@AutoBind
public class BlobCacheImpl implements BlobCache {

  private final Path tmpDirectory;

  @Inject
  BlobCacheImpl(AppContext appContext) {
    this.tmpDirectory = appContext.getTmpDir().resolve("_store_/locals");
  }

  @Override
  public Optional<Path> get(Path path, String eTag) throws IOException {
    Path cachePath = getCachePath(path, eTag);

    if (Files.isRegularFile(cachePath)) {
      return Optional.of(cachePath);
    }

    return Optional.empty();
  }

  @Override
  public Path put(Path path, String eTag, InputStream content) throws IOException {
    Path cachePath = getCachePath(path, eTag);

    Files.createDirectories(cachePath.getParent());
    Files.copy(content, cachePath, StandardCopyOption.REPLACE_EXISTING);

    // NOPMD - TODO: cleanup older/other entries?

    return cachePath;
  }

  private Path getCachePath(Path path, String eTag) {
    String fileDir = path.getFileName().toString().replaceAll("\\.", "_");
    // The eTag comes from the (remote) source and becomes a path segment; reduce it to safe
    // filename
    // characters so it cannot contain separators or `..`.
    String safeETag = eTag.replaceAll("[^A-Za-z0-9._-]", "_");

    Path cachePath =
        tmpDirectory.resolve(path.getParent()).resolve(fileDir).resolve(safeETag).normalize();

    // Defense-in-depth: `path` may carry `..` segments; ensure the cache entry stays inside the
    // cache directory.
    if (!cachePath.startsWith(tmpDirectory.normalize())) {
      throw new IllegalArgumentException(
          "The cache path for '" + path + "' would escape the cache directory.");
    }

    return cachePath;
  }
}
