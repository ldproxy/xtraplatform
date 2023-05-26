/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.infra;

import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.StoreSource;
import de.ii.xtraplatform.web.domain.HttpClient;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HttpFetcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpFetcher.class);

  private final Path tmpDirectory;
  private final HttpClient httpClient;

  public HttpFetcher(Path tmpDirectory, HttpClient httpClient) {
    this.tmpDirectory = tmpDirectory.resolve("_store_/cache/http");
    this.httpClient = httpClient;
  }

  boolean isAvailable(StoreSource storeSource) {
    try {
      new URI(storeSource.getSrc());
      // TODO: HEAD request
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
    return true;
  }

  Optional<Path> load(StoreSource storeSource) {
    Path cachePath = getAbsolutePath(storeSource);

    if (storeSource.getArchiveCache() && Files.exists(cachePath)) {
      return Optional.of(cachePath);
    }

    URI uri = URI.create(storeSource.getSrc());
    InputStream asInputStream;

    try {
      asInputStream = httpClient.getAsInputStream(uri.toString());
    } catch (Throwable e) {
      LogContext.error(LOGGER, e, "Could not load HTTP store source from {}", storeSource.getSrc());
      return Optional.empty();
    }

    try {
      Files.createDirectories(cachePath.getParent());
      Files.copy(asInputStream, cachePath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      LogContext.error(LOGGER, e, "Could not cache HTTP store source to {}", cachePath);
      return Optional.empty();
    }

    return Optional.of(cachePath);
  }

  Path getAbsolutePath(StoreSource storeSource) {
    URI uri = URI.create(storeSource.getSrc());
    Path src = Path.of("/").relativize(Path.of(uri.getPath()));

    return tmpDirectory.resolve(src);
  }
}
