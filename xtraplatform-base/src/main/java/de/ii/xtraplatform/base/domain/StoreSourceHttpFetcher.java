/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.client.HttpClientConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Minutes;

public class StoreSourceHttpFetcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(StoreSourceHttpFetcher.class);
  private static final Map<Path, Long> PULLED = Collections.synchronizedMap(new LinkedHashMap<>());

  private final Path tmpDirectory;
  private final CloseableHttpClient httpClient;

  public StoreSourceHttpFetcher(Path tmpDirectory, HttpClientConfiguration httpClientCfg) {
    this.tmpDirectory = tmpDirectory.resolve("_store_/cache/http");
    this.httpClient =
        new HttpClientBuilder(new MetricRegistry()).using(httpClientCfg).build("store");
  }

  public boolean isAvailable(StoreSource storeSource) {
    try {
      new URI(storeSource.getSrc());
      // TODO: HEAD request
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
    return true;
  }

  public Optional<Path> load(StoreSource storeSource) {
    Path cachePath = getAbsolutePath(storeSource);

    if (storeSource.getArchiveCache()
        && Files.exists(cachePath)
        && isNewerThan(cachePath, Minutes.of(5))) {
      return Optional.of(cachePath);
    }

    URI uri = URI.create(storeSource.getSrc());

    try (InputStream asInputStream = getAsInputStream(uri.toString())) {
      Files.createDirectories(cachePath.getParent());
      Files.copy(asInputStream, cachePath, StandardCopyOption.REPLACE_EXISTING);
      PULLED.put(cachePath, Instant.now().toEpochMilli());
    } catch (Throwable e) {
      LogContext.error(
          LOGGER, e, "Could not load or cache HTTP store source from {}", storeSource.getSrc());
      return Optional.empty();
    }

    return Optional.of(cachePath);
  }

  Path getAbsolutePath(StoreSource storeSource) {
    URI uri = URI.create(storeSource.getSrc());
    Path src = Path.of("/").relativize(Path.of(uri.getPath()));

    return tmpDirectory.resolve(src);
  }

  private InputStream getAsInputStream(String url) {
    try (CloseableHttpResponse response = httpClient.execute(new HttpGet(url))) {
      byte[] data = response.getEntity().getContent().readAllBytes();
      return new java.io.ByteArrayInputStream(data);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private boolean isNewerThan(Path cachePath, TemporalAmount temporalAmount) {
    return Instant.now().minus(temporalAmount).toEpochMilli() < PULLED.getOrDefault(cachePath, 0L);
  }
}
