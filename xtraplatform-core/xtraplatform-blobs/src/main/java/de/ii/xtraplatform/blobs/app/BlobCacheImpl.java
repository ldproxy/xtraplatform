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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class BlobCacheImpl implements BlobCache {

  private final Path tmpDirectory;

  @Inject
  BlobCacheImpl(AppContext appContext) {
    this.tmpDirectory = appContext.getTmpDir();
  }

  @Override
  public Path save(InputStream content, Path path) throws IOException {
    Path cachePath = getDirectory(path).resolve(path.getFileName());

    Files.createDirectories(cachePath.getParent());
    Files.copy(content, cachePath, StandardCopyOption.REPLACE_EXISTING);

    return cachePath;
  }

  private Path getDirectory(Path blobPath) {
    String fileDir = blobPath.getFileName().toString().replaceAll("\\.", "_");

    return tmpDirectory.resolve("blobs").resolve(blobPath.getParent()).resolve(fileDir);
  }
}
