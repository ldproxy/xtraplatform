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
    try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipFile))) {
      ZipEntry zipEntry = zip.getNextEntry();
      while (zipEntry != null) {
        if (!zipEntry.isDirectory() && !zipEntry.getName().startsWith("META-INF")) {
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
    }
  }
}
