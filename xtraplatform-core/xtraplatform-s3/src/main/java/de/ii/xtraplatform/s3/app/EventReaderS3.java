/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.s3.app;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.supplierMayThrow;

import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.base.domain.StoreDriver;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.entities.domain.EventReader;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EventReaderS3 implements EventReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventReaderS3.class);

  private final MinioClient minioClient;

  EventReaderS3(MinioClient minioClient) {
    this.minioClient = minioClient;
  }

  @Override
  public Stream<Tuple<Path, Supplier<byte[]>>> load(
      Path sourcePath, List<String> includes, List<String> excludes) throws IOException {
    Path bucket = sourcePath.getName(0);
    String prefix =
        sourcePath.getNameCount() > 1
            ? sourcePath.subpath(1, sourcePath.getNameCount()).toString()
            : "";
    List<PathMatcher> includeMatchers = StoreDriver.asMatchers(includes, prefix);
    List<PathMatcher> excludeMatchers = StoreDriver.asMatchers(excludes, prefix);

    if (LOGGER.isDebugEnabled(MARKER.S3)) {
      LOGGER.debug("S3 loading events from {}", sourcePath);
    }

    return loadPathStream(bucket, prefix, includeMatchers, excludeMatchers)
        .map(path -> Tuple.of(path, supplierMayThrow(() -> readPayload(path))));
  }

  private Stream<Path> loadPathStream(
      Path bucket, String prefix, List<PathMatcher> includes, List<PathMatcher> excludes)
      throws IOException {

    Spliterator<Result<Item>> results =
        minioClient
            .listObjects(
                ListObjectsArgs.builder()
                    .bucket(bucket.toString())
                    .prefix(prefix)
                    .recursive(true)
                    .build())
            .spliterator();

    return StreamSupport.stream(results, false)
        .flatMap(
            result -> {
              try {
                return Stream.of(result.get());
              } catch (Throwable e) {
                return Stream.empty();
              }
            })
        .filter(
            item ->
                (includes.isEmpty()
                        || includes.stream()
                            .anyMatch(include -> include.matches(Path.of(item.objectName()))))
                    && excludes.stream()
                        .noneMatch(exclude -> exclude.matches(Path.of(item.objectName()))))
        .map(item -> bucket.resolve(item.objectName()));
  }

  private byte[] readPayload(Path path) throws IOException {
    try {
      return minioClient
          .getObject(
              GetObjectArgs.builder()
                  .bucket(path.getName(0).toString())
                  .object(path.subpath(1, path.getNameCount()).toString())
                  .build())
          .readAllBytes();
    } catch (Throwable e) {
      throw new IOException("S3 Driver", e);
    }
  }
}
