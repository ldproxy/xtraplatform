/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.s3.app;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.supplierMayThrow;

import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.blobs.domain.Blob;
import de.ii.xtraplatform.blobs.domain.BlobCache;
import de.ii.xtraplatform.blobs.domain.BlobLocals;
import de.ii.xtraplatform.blobs.domain.BlobSource;
import de.ii.xtraplatform.blobs.domain.BlobWriter;
import de.ii.xtraplatform.blobs.domain.ImmutableBlob;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiPredicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.ws.rs.core.EntityTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlobSourceS3 implements BlobSource, BlobWriter, BlobLocals, Closeable {
  private static final Logger LOGGER = LoggerFactory.getLogger(BlobSourceS3.class);

  private final MinioClient minioClient;
  private final String bucket;
  private final S3OperationsHelper s3Operations;
  private final CacheOperationsHelper cacheOperations;
  private final PathHelper pathHelper;

  @Override
  public void close() throws IOException {
    if (minioClient != null) {
      try {
        minioClient.close();
      } catch (Exception e) {
        throw new IOException("Failed to close MinioClient", e);
      }
    }
  }

  public BlobSourceS3(MinioClient minioClient, String bucket, Path root, BlobCache cache) {
    this(minioClient, bucket, root, cache, null);
  }

  public BlobSourceS3(
      MinioClient minioClient, String bucket, Path root, BlobCache cache, Path prefix) {
    this.minioClient = minioClient;
    this.bucket = bucket;
    this.s3Operations = new S3OperationsHelper(minioClient, bucket);
    this.cacheOperations = new CacheOperationsHelper(cache);
    this.pathHelper = new PathHelper(root, prefix);
  }

  @Override
  public boolean canHandle(Path path) {
    return pathHelper.canHandle(path);
  }

  @Override
  public boolean has(Path path) throws IOException {
    return s3Operations.getStat(pathHelper.full(path)).isPresent();
  }

  @Override
  public Optional<InputStream> content(Path path) throws IOException {
    return s3Operations.getCurrent(pathHelper.full(path));
  }

  @Override
  public Stream<Path> walk(Path path, int maxDepth, BiPredicate<Path, PathAttributes> matcher)
      throws IOException {
    if (!pathHelper.canHandle(path) || maxDepth <= 0) {
      return Stream.empty();
    }

    if (LOGGER.isDebugEnabled(MARKER.S3)) {
      LOGGER.debug(MARKER.S3, "S3 walk {}", path);
    }

    Path prefix = Path.of(pathHelper.full(path));

    Set<Path> paths = new HashSet<>();

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
        .map(item -> prefix.relativize(Path.of(item.objectName())))
        .flatMap(
            item -> {
              if (item.getNameCount() <= 1) {
                return Stream.of(item);
              }

              return IntStream.rangeClosed(1, Math.min(maxDepth, item.getNameCount()))
                  .mapToObj(
                      i -> {
                        Path subPath = item.subpath(0, i);
                        boolean added = paths.add(subPath);

                        if (!added) {
                          return null;
                        }

                        boolean isValue = i == item.getNameCount();

                        boolean matches =
                            matcher.test(
                                subPath,
                                new PathAttributes() {
                                  @Override
                                  public boolean isValue() {
                                    return isValue;
                                  }

                                  @Override
                                  public boolean isHidden() {
                                    return isValue
                                        && subPath.getFileName().toString().startsWith(".");
                                  }
                                });

                        if (!matches) {
                          return null;
                        }

                        return subPath;
                      })
                  .filter(Objects::nonNull);
            });
  }

  @Override
  public void put(Path path, InputStream content) throws IOException {
    if (!pathHelper.canHandle(path)) {
      return;
    }

    try (ByteArrayInputStream buffer = new ByteArrayInputStream(content.readAllBytes())) {
      if (LOGGER.isDebugEnabled(MARKER.S3)) {
        LOGGER.debug(MARKER.S3, "S3 put content {}", path);
      }

      minioClient.putObject(
          PutObjectArgs.builder().bucket(bucket).object(pathHelper.full(path)).stream(
                  buffer, buffer.available(), -1)
              .build());
    } catch (Throwable e) {
      throw new IOException("S3 Driver", e);
    }
  }

  @Override
  public void delete(Path path) throws IOException {
    if (!pathHelper.canHandle(path)) {
      return;
    }

    try {
      if (LOGGER.isDebugEnabled(MARKER.S3)) {
        LOGGER.debug(MARKER.S3, "S3 delete content {}", path);
      }

      minioClient.removeObject(
          RemoveObjectArgs.builder().bucket(bucket).object(pathHelper.full(path)).build());
    } catch (Throwable e) {
      throw new IOException("S3 Driver", e);
    }
  }

  @Override
  public Optional<Blob> get(Path path) throws IOException {
    return s3Operations
        .getStat(pathHelper.full(path))
        .map(
            stat ->
                ImmutableBlob.of(
                    path,
                    stat.size(),
                    stat.lastModified().toInstant().toEpochMilli(),
                    Optional.of(new EntityTag(stat.etag())),
                    Optional.ofNullable(stat.contentType()),
                    supplierMayThrow(
                        () ->
                            content(path)
                                .orElseThrow(
                                    () ->
                                        new IOException(
                                            "Unexpected error, could not get " + path)))));
  }

  @Override
  public long size(Path path) throws IOException {
    return s3Operations.getStat(pathHelper.full(path)).map(StatObjectResponse::size).orElse(-1L);
  }

  @Override
  public long lastModified(Path path) throws IOException {
    return s3Operations
        .getStat(pathHelper.full(path))
        .map(stat -> stat.lastModified().toInstant().toEpochMilli())
        .orElse(-1L);
  }

  @Override
  public Optional<Path> asLocalPath(Path path, boolean writable) throws IOException {
    if (writable) {
      throw new IOException("Local resources from S3 cannot be written to");
    }

    Optional<StatObjectResponse> stat = s3Operations.getStat(pathHelper.full(path));
    if (stat.isEmpty()) {
      return Optional.empty();
    }
    String eTag = stat.get().etag();
    Optional<Path> cachePath = cacheOperations.getCachedPath(path, eTag);
    if (cachePath.isPresent()) {
      return cachePath;
    }
    return cacheOperations.updateCacheAndReturnPath(path, eTag, content(path));
  }
}
