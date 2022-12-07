/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain.util;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.supplierMayThrow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public interface ZipWalker {

  static void walkEntries(Path zipFile, BiConsumer<Path, Supplier<byte[]>> entryHandler)
      throws IOException {
    ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipFile));
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
        try {
          entryHandler.accept(
              Path.of("/", zipEntry.getName()), supplierMayThrow(zip::readAllBytes));
        } catch (RuntimeException e) {
          if (Objects.nonNull(e.getCause()) && e.getCause() instanceof IOException) {
            throw (IOException) e.getCause();
          }
          throw e;
        }
      }
      zip.closeEntry();
      zipEntry = zip.getNextEntry();
    }
    zip.close();
  }
}
