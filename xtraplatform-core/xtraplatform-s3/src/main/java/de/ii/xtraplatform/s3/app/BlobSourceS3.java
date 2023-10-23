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
import io.minio.GetObjectArgs;
import io.minio.GetObjectArgs.Builder;
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
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiPredicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import javax.ws.rs.core.EntityTag;
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
    return getStat(path).isPresent();
  }

  @Override
  public Optional<InputStream> content(Path path) throws IOException {
    return getCurrent(path);
  }

  @Override
  public Optional<Blob> get(Path path) throws IOException {
    return getStat(path)
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
    return getStat(path).map(StatObjectResponse::size).orElse(-1L);
  }

  @Override
  public long lastModified(Path path) throws IOException {
    return getStat(path).map(stat -> stat.lastModified().toInstant().toEpochMilli()).orElse(-1L);
  }

  // TODO: walkInfo
  @Override
  public Stream<Path> walk(Path path, int maxDepth, BiPredicate<Path, PathAttributes> matcher)
      throws IOException {
    if (!canHandle(path) || maxDepth <= 0) {
      return Stream.empty();
    }

    if (LOGGER.isDebugEnabled(MARKER.S3)) {
      LOGGER.debug("S3 walk {}", path);
    }

    Path prefix = Path.of(full(path));

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

    // TODO: concat path?
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
    if (!canHandle(path)) {
      return;
    }

    try (ByteArrayInputStream buffer = new ByteArrayInputStream(content.readAllBytes())) {
      if (LOGGER.isDebugEnabled(MARKER.S3)) {
        LOGGER.debug("S3 put content {}", path);
      }

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
      if (LOGGER.isDebugEnabled(MARKER.S3)) {
        LOGGER.debug("S3 delete content {}", path);
      }

      minioClient.removeObject(
          RemoveObjectArgs.builder().bucket(bucket).object(full(path)).build());
    } catch (Throwable e) {
      throw new IOException("S3 Driver", e);
    }
  }

  @Override
  public Optional<Path> asLocalPath(Path path, boolean writable) throws IOException {
    if (writable) {
      throw new IOException("Local resources from S3 cannot be written to");
    }

    Optional<StatObjectResponse> stat = getStat(path);

    if (stat.isPresent()) {
      String eTag = stat.get().etag();
      Optional<Path> cachePath = cache.get(path, eTag);

      if (cachePath.isPresent()) {
        if (LOGGER.isDebugEnabled(MARKER.S3)) {
          LOGGER.debug("S3 using local cache {}", cachePath.get());
        }
        return cachePath;
      }

      Optional<InputStream> content = content(path);

      if (content.isPresent()) {
        return Optional.of(cache.put(path, eTag, content.get()))
            .map(
                p -> {
                  if (LOGGER.isDebugEnabled(MARKER.S3)) {
                    LOGGER.debug("S3 updating local cache {}", p);
                  }

                  return p;
                });
      }
    }

    return Optional.empty();
  }

  private Optional<StatObjectResponse> getStat(Path path) {
    if (!canHandle(path)) {
      return Optional.empty();
    }

    if (LOGGER.isDebugEnabled(MARKER.S3)) {
      LOGGER.debug("S3 get stat {}", path);
    }

    try {
      return Optional.of(
          minioClient.statObject(
              StatObjectArgs.builder().bucket(bucket).object(full(path)).build()));
    } catch (Throwable e) {
      return Optional.empty();
    }
  }

  public Optional<InputStream> getCurrent(Path path) throws IOException {
    return getByETag(path, null);
  }

  public Optional<InputStream> getByETag(Path path, String eTag) {
    if (!canHandle(path)) {
      return Optional.empty();
    }

    if (LOGGER.isDebugEnabled(MARKER.S3)) {
      LOGGER.debug(
          "S3 get content {} {}", path, Objects.nonNull(eTag) ? "if-none-match " + eTag : "");
    }

    Builder builder = GetObjectArgs.builder().bucket(bucket).object(full(path));

    if (Objects.nonNull(eTag)) {
      builder.notMatchETag(eTag);
    }

    try {
      return Optional.of(minioClient.getObject(builder.build()));
    } catch (Throwable e) {
      return Optional.empty();
      // throw new IOException("S3 Driver", e);
    }
  }

  private String full(Path path) {
    return Objects.isNull(prefix)
        ? root.resolve(path).toString()
        : root.resolve(prefix.relativize(path)).toString();
  }
}
