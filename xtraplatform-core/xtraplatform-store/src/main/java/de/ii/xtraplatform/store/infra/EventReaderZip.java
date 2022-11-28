/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.infra;

import de.ii.xtraplatform.base.domain.util.Tuple;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class EventReaderZip implements EventReader {

  @Override
  public Stream<Tuple<Path, Supplier<byte[]>>> load(Path sourcePath) throws IOException {
    List<Tuple<Path, Supplier<byte[]>>> entries = new ArrayList<>();

    walkEntries(sourcePath, (zipEntry, payload) -> entries.add(Tuple.of(zipEntry, () -> payload)));

    return entries.stream();
  }

  private void walkEntries(Path pkg, BiConsumer<Path, byte[]> entryHandler) throws IOException {
    ZipInputStream zip = new ZipInputStream(Files.newInputStream(pkg));
    /*JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(pkg), true);
    Manifest manifest = jarInputStream.getManifest();

    if (Objects.isNull(manifest)) {
      throw new IllegalArgumentException(String.format("package '%s' has no manifest", pkg));
    }*/

    ZipEntry zipEntry = zip.getNextEntry();
    while (zipEntry != null) {
      if (!zipEntry.isDirectory() && !zipEntry.getName().startsWith("META-INF")) {
        /*Attributes attributes = manifest.getAttributes(zipEntry.getName());
        if (Objects.isNull(attributes)) {
          throw new IllegalArgumentException(
              String.format(
                  "entry '%s' in package '%s' is not contained in manifest",
                  zipEntry.getName(), pkg));
        }
        attributes.forEach((key, value) -> LOGGER.error("ATT {} {}", key, value));
        */
        entryHandler.accept(Path.of("/", zipEntry.getName()), zip.readAllBytes());
      }
      zip.closeEntry();
      zipEntry = zip.getNextEntry();
    }
    zip.close();
  }
}
