/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.infra;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.StoreSource;
import de.ii.xtraplatform.base.domain.StoreSource.Content;
import de.ii.xtraplatform.base.domain.StoreSource.Type;
import de.ii.xtraplatform.store.domain.BlobSource;
import de.ii.xtraplatform.store.domain.BlobStoreDriver;
import de.ii.xtraplatform.web.domain.Http;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class BlobStoreDriverHttp implements BlobStoreDriver {

  private static final Logger LOGGER = LoggerFactory.getLogger(BlobStoreDriverHttp.class);

  private final BlobExtractor blobExtractor;
  private final Path tmpDirectory;
  private final HttpFetcher httpFetcher;

  @Inject
  BlobStoreDriverHttp(AppContext appContext, Http http) {
    this.blobExtractor = new ZipReader();
    this.tmpDirectory = appContext.getTmpDir();
    this.httpFetcher = new HttpFetcher(appContext.getTmpDir(), http.getDefaultClient());
  }

  @Override
  public Type getType() {
    return Type.HTTP;
  }

  @Override
  public boolean isAvailable(StoreSource storeSource) {
    return httpFetcher.isAvailable(storeSource);
  }

  @Override
  public BlobSource init(StoreSource storeSource) throws IOException {
    if (!storeSource.isArchive()) {
      LOGGER.error("Store source {} only supports archives.", storeSource.getLabel());
      return new BlobSourceEmpty();
    }

    Optional<Path> archivePath = httpFetcher.load(storeSource);

    if (archivePath.isEmpty()) {
      return new BlobSourceEmpty();
    }

    Path root = getDirectory(archivePath.get());
    Path extractRoot =
        storeSource.isSingleContent() ? root.resolve(storeSource.getPrefix().orElse("")) : root;

    blobExtractor.extract(
        archivePath.get(),
        Path.of(storeSource.getArchiveRoot()),
        entry -> storeSource.isSingleContent() || entry.startsWith(Content.RESOURCES.getPrefix()),
        extractRoot,
        !storeSource.getArchiveCache());

    if (!storeSource.isSingleContent()) {
      root = root.resolve(Content.RESOURCES.getPrefix());
    }

    BlobSource blobSource =
        storeSource.isSingleContent() && storeSource.getPrefix().isPresent()
            ? new BlobSourceFs(root, Path.of(storeSource.getPrefix().get()))
            : new BlobSourceFs(root);

    return blobSource;
  }

  private Path getDirectory(Path archivePath) {
    String archiveName = archivePath.getFileName().toString().replaceAll("\\.", "_");

    return tmpDirectory.resolve("blobs").resolve(archiveName);
  }
}
