/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import de.ii.xtraplatform.base.domain.StoreSource.Type;
import io.dropwizard.client.HttpClientConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CfgStoreDriverHttp implements CfgStoreDriver {

  private static final Logger LOGGER = LoggerFactory.getLogger(CfgStoreDriverHttp.class);

  private final StoreSourceHttpFetcher httpFetcher;

  public CfgStoreDriverHttp(Path tmpDir, HttpClientConfiguration httpClient) {
    this.httpFetcher = new StoreSourceHttpFetcher(tmpDir, httpClient);
  }

  @Override
  public String getType() {
    return Type.HTTP_KEY;
  }

  @Override
  public boolean isAvailable(StoreSource storeSource) {
    return httpFetcher.isAvailable(storeSource);
  }

  @Override
  public Optional<InputStream> load(StoreSource storeSource) throws IOException {
    if (!storeSource.isArchive()) {
      LOGGER.error("Store source {} only supports archives.", storeSource.getLabel());
      return Optional.empty();
    }

    Optional<Path> archivePath = httpFetcher.load(storeSource);

    if (archivePath.isEmpty()) {
      return Optional.empty();
    }

    return CfgStoreDriverFs.loadFromZip(archivePath.get(), storeSource.getArchiveRoot());
  }
}
