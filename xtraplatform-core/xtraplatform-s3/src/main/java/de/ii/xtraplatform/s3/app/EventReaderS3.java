/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.s3.app;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.supplierMayThrow;

import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.entities.domain.EventReader;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import java.io.IOException;
import java.nio.file.Path;
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
  public Stream<Tuple<Path, Supplier<byte[]>>> load(Path sourcePath) throws IOException {
    return loadPathStream(sourcePath)
        .map(path -> Tuple.of(path, supplierMayThrow(() -> readPayload(path))));
  }

  private Stream<Path> loadPathStream(Path path) throws IOException {
    Path bucket = path.getName(0);
    LOGGER.debug(
        "S3 LOAD {} - {} - {}",
        path,
        bucket,
        (path.getNameCount() > 1 ? path.subpath(1, path.getNameCount()).toString() : ""));
    Spliterator<Result<Item>> results =
        minioClient
            .listObjects(
                ListObjectsArgs.builder()
                    .bucket(bucket.toString())
                    .prefix(
                        path.getNameCount() > 1
                            ? path.subpath(1, path.getNameCount()).toString()
                            : "")
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
        .map(
            item -> {
              Path resolved = bucket.resolve(item.objectName());
              LOGGER.debug("S3 EVENT {}", resolved);
              return resolved;
            });
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
      throw new IOException("minio error", e);
    }
  }
}
