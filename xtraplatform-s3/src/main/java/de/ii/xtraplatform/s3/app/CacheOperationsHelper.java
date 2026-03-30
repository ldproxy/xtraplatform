/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.s3.app;

import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.blobs.domain.BlobCache;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CacheOperationsHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheOperationsHelper.class);

  private final BlobCache cache;

  CacheOperationsHelper(BlobCache cache) {
    this.cache = cache;
  }

  Optional<Path> getCachedPath(Path path, String eTag) throws IOException {
    Optional<Path> cachePath = cache.get(path, eTag);
    if (cachePath.isPresent()) {
      if (LOGGER.isDebugEnabled(MARKER.S3)) {
        LOGGER.debug(MARKER.S3, "S3 using local cache {}", cachePath.get());
      }
      return cachePath;
    }
    return Optional.empty();
  }

  Optional<Path> updateCacheAndReturnPath(Path path, String eTag, Optional<InputStream> content)
      throws IOException {
    if (content.isEmpty()) {
      return Optional.empty();
    }
    if (LOGGER.isDebugEnabled(MARKER.S3)) {
      LOGGER.debug(MARKER.S3, "S3 updating local cache for {}", path);
    }
    Path p = cache.put(path, eTag, content.get());
    if (LOGGER.isDebugEnabled(MARKER.S3)) {
      LOGGER.debug(MARKER.S3, "S3 updated local cache {}", p);
    }
    return Optional.of(p);
  }
}
