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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class BlobStoreDriverFs implements BlobStoreDriver {

  private static final Logger LOGGER = LoggerFactory.getLogger(BlobStoreDriverFs.class);

  private final BlobExtractor blobExtractor;
  private final Path dataDirectory;
  private final Path tmpDirectory;

  @Inject
  BlobStoreDriverFs(AppContext appContext) {
    this.blobExtractor = new ZipReader();
    this.dataDirectory = appContext.getDataDir();
    this.tmpDirectory = appContext.getTmpDir();
  }

  @Override
  public Type getType() {
    return Type.FS;
  }

  @Override
  public boolean isAvailable(StoreSource storeSource) {
    return storeSource.isArchive()
        ? Files.isRegularFile(getAbsolutePath(dataDirectory, storeSource))
        : Files.isDirectory(getAbsolutePath(dataDirectory, storeSource));
  }

  @Override
  public BlobSource init(StoreSource storeSource) throws IOException {
    Path root = getAbsolutePath(dataDirectory, storeSource);

    if (storeSource.isArchive()) {
      Path archivePath = root;
      String archiveName = archivePath.getFileName().toString().replaceAll("\\.", "_");
      root = tmpDirectory.resolve("blobs").resolve(archiveName);
      Path extractRoot =
          storeSource.isSingleContent() ? root.resolve(storeSource.getPrefix().orElse("")) : root;

      blobExtractor.extract(
          archivePath,
          Path.of(storeSource.getArchiveRoot()),
          entry -> storeSource.isSingleContent() || entry.startsWith(Content.RESOURCES.getPrefix()),
          extractRoot,
          !storeSource.getArchiveCache());
    }

    if (!storeSource.isSingleContent()) {
      root = root.resolve(Content.RESOURCES.getPrefix());
    }

    BlobSource blobSource =
        storeSource.isSingleContent() && storeSource.getPrefix().isPresent()
            ? new BlobSourceFs(root, Path.of(storeSource.getPrefix().get()))
            : new BlobSourceFs(root);

    return blobSource;
  }

  private static Path getAbsolutePath(Path dataDir, StoreSource storeSource) {
    Path src = Path.of(storeSource.getSrc());

    return src.isAbsolute() ? src : dataDir.resolve(src);
  }
}
