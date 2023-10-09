/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import de.ii.xtraplatform.base.domain.StoreSource.Type;
import de.ii.xtraplatform.base.domain.util.ZipWalker;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CfgStoreDriverFs implements CfgStoreDriver {

  private static final Logger LOGGER = LoggerFactory.getLogger(CfgStoreDriverFs.class);
  static final Path CFG_YML = Path.of("cfg.yml");
  private final Path dataDirectory;

  public CfgStoreDriverFs(Path dataDirectory) {
    this.dataDirectory = dataDirectory;
  }

  @Override
  public String getType() {
    return Type.FS_KEY;
  }

  // TODO: same as in BlobStoreDriverFs
  @Override
  public boolean isAvailable(StoreSource storeSource) {
    Path absolutePath = getAbsolutePath(dataDirectory, storeSource);

    return storeSource.isArchive() || storeSource.isSingleContent()
        ? Files.isRegularFile(absolutePath)
        : Files.isDirectory(absolutePath);
  }

  // TODO: single content?
  @Override
  public Optional<InputStream> load(StoreSource storeSource) throws IOException {
    Path root = getAbsolutePath(dataDirectory, storeSource);

    if (storeSource.isArchive()) {
      return loadFromZip(root, storeSource.getArchiveRoot());
    }

    Path cfg = storeSource.isSingleContent() ? root : root.resolve(CFG_YML);

    if (Files.isRegularFile(cfg)) {
      return Optional.of(new ByteArrayInputStream(Files.readAllBytes(cfg)));
    }

    return Optional.empty();
  }

  public static Optional<InputStream> loadFromZip(Path zipPath, String archiveRoot)
      throws IOException {
    List<InputStream> entries = new ArrayList<>();
    Path cfgYml = Path.of(archiveRoot).resolve(CFG_YML);

    ZipWalker.walkEntries(
        zipPath,
        (zipEntry, payload) -> {
          if (Objects.equals(zipEntry, cfgYml)) {
            entries.add(new ByteArrayInputStream(payload.get()));
          }
        });

    return entries.stream().findFirst();
  }

  private static Path getAbsolutePath(Path dataDir, StoreSource storeSource) {
    Path src = Path.of(storeSource.getSrc());

    return src.isAbsolute() ? src : dataDir.resolve(src);
  }
}
