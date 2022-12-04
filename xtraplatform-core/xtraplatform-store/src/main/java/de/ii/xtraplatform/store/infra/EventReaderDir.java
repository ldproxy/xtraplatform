/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.infra;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.supplierMayThrow;

import de.ii.xtraplatform.base.domain.util.Tuple;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.stream.Stream;

class EventReaderDir implements EventReader {

  @Override
  public Stream<Tuple<Path, Supplier<byte[]>>> load(Path sourcePath) throws IOException {
    return loadPathStream(sourcePath)
        .map(path -> Tuple.of(path, supplierMayThrow(() -> readPayload(path))));
  }

  private static Stream<Path> loadPathStream(Path directory) throws IOException {
    return Files.find(
        directory, 32, (path, basicFileAttributes) -> basicFileAttributes.isRegularFile());
  }

  private static byte[] readPayload(Path path) throws IOException {
    return Files.readAllBytes(path);
  }
}
