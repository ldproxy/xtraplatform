/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.infra;

import de.ii.xtraplatform.base.domain.util.LambdaWithException;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.base.domain.util.ZipWalker;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipReader implements EventReader, BlobExtractor {

  private static final Logger LOGGER = LoggerFactory.getLogger(ZipReader.class);

  @Override
  public Stream<Tuple<Path, Supplier<byte[]>>> load(Path sourcePath) throws IOException {
    List<Tuple<Path, Supplier<byte[]>>> entries = new ArrayList<>();

    ZipWalker.walkEntries(
        sourcePath,
        (zipEntry, payload) -> {
          // have to read here because zip is closed after return of entries
          byte[] bytes = payload.get();
          entries.add(Tuple.of(zipEntry, () -> bytes));
        });

    return entries.stream();
  }

  @Override
  public void extract(
      Path archiveFile,
      Path archiveRoot,
      Predicate<Path> includeEntry,
      Path targetRoot,
      boolean overwrite)
      throws IOException {
    if (overwrite) {
      // TODO: delete target directory
    }

    ZipWalker.walkEntries(
        archiveFile,
        LambdaWithException.biConsumerMayThrow(
            (zipEntry, payload) -> {
              Path entry = archiveRoot.relativize(zipEntry);
              Path target = targetRoot.resolve(entry);

              if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Extracting to {} {}", target, includeEntry.test(entry));
              }

              if (includeEntry.test(entry) && (overwrite || !Files.exists(target))) {
                Files.createDirectories(target.getParent());

                try (OutputStream out = Files.newOutputStream(target)) {
                  out.write(payload.get());
                }
              }
            }));
  }
}