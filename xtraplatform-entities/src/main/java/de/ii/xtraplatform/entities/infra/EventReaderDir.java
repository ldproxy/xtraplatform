/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.infra;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.supplierMayThrow;

import de.ii.xtraplatform.base.domain.StoreDriver;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.entities.domain.EventReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EventReaderDir implements EventReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventReaderDir.class);

  @Override
  public Stream<Tuple<Path, Supplier<byte[]>>> load(
      Path sourcePath, List<String> includes, List<String> excludes) throws IOException {
    List<PathMatcher> includeMatchers = StoreDriver.asMatchers(includes, sourcePath.toString());
    List<PathMatcher> excludeMatchers = StoreDriver.asMatchers(excludes, sourcePath.toString());

    return loadPathStream(sourcePath, includeMatchers, excludeMatchers)
        .map(path -> Tuple.of(path, supplierMayThrow(() -> readPayload(path))));
  }

  private static Stream<Path> loadPathStream(
      Path directory, List<PathMatcher> includes, List<PathMatcher> excludes) throws IOException {
    return Files.find(
        directory,
        32,
        (path, basicFileAttributes) ->
            basicFileAttributes.isRegularFile()
                && (includes.isEmpty()
                    || includes.stream().anyMatch(include -> include.matches(path)))
                && excludes.stream().noneMatch(exclude -> exclude.matches(path)));
  }

  private static byte[] readPayload(Path path) throws IOException {
    return Files.readAllBytes(path);
  }
}
