/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.s3.app;

import de.ii.xtraplatform.blobs.domain.BlobCache;
import de.ii.xtraplatform.blobs.domain.BlobLocals;
import de.ii.xtraplatform.blobs.domain.BlobSource;
import de.ii.xtraplatform.blobs.domain.BlobWriter;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlobSourceS3 implements BlobSource, BlobWriter, BlobLocals {

  private static final Logger LOGGER = LoggerFactory.getLogger(BlobSourceS3.class);

  private final MinioClient minioClient;
  private final String bucket;
  private final Path root;
  @Nullable private final Path prefix;
  private final BlobCache cache;

  public BlobSourceS3(MinioClient minioClient, String bucket, Path root, BlobCache cache) {
    this(minioClient, bucket, root, cache, null);
  }

  public BlobSourceS3(
      MinioClient minioClient, String bucket, Path root, BlobCache cache, Path prefix) {
    this.minioClient = minioClient;
    this.bucket = bucket;
    this.root = root;
    this.cache = cache;
    this.prefix = prefix;
  }

  @Override
  public boolean canHandle(Path path) {
    return Objects.isNull(prefix) || path.startsWith(prefix);
  }

  @Override
  public boolean has(Path path) throws IOException {
    if (!canHandle(path)) {
      return false;
    }

    LOGGER.debug("HAS {}", path);

    try {
      StatObjectResponse objectStat =
          minioClient.statObject(
              StatObjectArgs.builder().bucket(bucket).object(full(path)).build());
      return true;
    } catch (Throwable e) {
      return false;
    }
  }

  @Override
  public Optional<InputStream> get(Path path) throws IOException {
    if (!canHandle(path)) {
      return Optional.empty();
    }

    LOGGER.debug("GET {}", path);

    try {
      return Optional.of(
          minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(full(path)).build()));
    } catch (Throwable e) {
      return Optional.empty();
    }
  }

  @Override
  public long size(Path path) throws IOException {
    if (!canHandle(path)) {
      return -1;
    }

    try {
      StatObjectResponse objectStat =
          minioClient.statObject(
              StatObjectArgs.builder().bucket(bucket).object(full(path)).build());

      return objectStat.size();
    } catch (Throwable e) {
      throw new IOException("S3 Driver", e);
    }
  }

  @Override
  public long lastModified(Path path) throws IOException {
    if (!canHandle(path)) {
      return -1;
    }

    try {
      StatObjectResponse objectStat =
          minioClient.statObject(
              StatObjectArgs.builder().bucket(bucket).object(full(path)).build());

      return objectStat.lastModified().toEpochSecond();
    } catch (Throwable e) {
      throw new IOException("S3 Driver", e);
    }
  }

  @Override
  public Stream<Path> walk(Path path, int maxDepth, BiPredicate<Path, PathAttributes> matcher)
      throws IOException {
    LOGGER.debug("WALK {}", path);
    if (!canHandle(path)) {
      return Stream.empty();
    }
    LOGGER.debug("WALK {}", path);

    Path prefix = Path.of(full(path));

    Spliterator<Result<Item>> results =
        minioClient
            .listObjects(
                ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix(prefix.toString())
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
              Path resolved = prefix.relativize(Path.of(item.objectName()));
              LOGGER.debug("S3 BLOB {}", resolved);
              return resolved;
            });
  }

  @Override
  public void put(Path path, InputStream content) throws IOException {
    if (!canHandle(path)) {
      return;
    }

    try (ByteArrayInputStream buffer = new ByteArrayInputStream(content.readAllBytes())) {
      minioClient.putObject(
          PutObjectArgs.builder().bucket(bucket).object(full(path)).stream(
                  buffer, buffer.available(), -1)
              .build());
    } catch (Throwable e) {
      throw new IOException("S3 Driver", e);
    }
  }

  @Override
  public void delete(Path path) throws IOException {
    if (!canHandle(path)) {
      return;
    }

    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder().bucket(bucket).object(full(path)).build());
    } catch (Throwable e) {
      throw new IOException("S3 Driver", e);
    }
  }

  // TODO: etags
  @Override
  public Optional<Path> asLocalPath(Path path, boolean writable) throws IOException {
    Optional<InputStream> content = get(path);

    if (content.isPresent()) {
      return Optional.of(cache.save(content.get(), path))
          .map(
              p -> {
                LOGGER.debug("GOT LOCAL {}", p);
                return p;
              });
    }

    return Optional.empty();
  }

  private String full(Path path) {
    return Objects.isNull(prefix)
        ? root.resolve(path).toString()
        : root.resolve(prefix.relativize(path)).toString();
  }
}
