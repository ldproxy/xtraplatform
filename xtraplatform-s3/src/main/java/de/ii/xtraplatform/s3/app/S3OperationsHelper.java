/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.s3.app;

import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class S3OperationsHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(S3OperationsHelper.class);

  private final MinioClient minioClient;
  private final String bucket;

  S3OperationsHelper(MinioClient minioClient, String bucket) {
    this.minioClient = minioClient;
    this.bucket = bucket;
  }

  Optional<StatObjectResponse> getStat(String fullPath) {
    if (LOGGER.isDebugEnabled(MARKER.S3)) {
      LOGGER.debug(MARKER.S3, "S3 get stat {}", fullPath);
    }

    try {
      return Optional.of(
          minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(fullPath).build()));
    } catch (Throwable e) {
      return Optional.empty();
    }
  }

  Optional<InputStream> getCurrent(String fullPath) throws IOException {
    return getByETag(fullPath, null);
  }

  Optional<InputStream> getByETag(String fullPath, String eTag) {
    if (LOGGER.isDebugEnabled(MARKER.S3)) {
      LOGGER.debug(
          MARKER.S3,
          "S3 get content {} {}",
          fullPath,
          Objects.nonNull(eTag) ? "if-none-match " + eTag : "");
    }

    GetObjectArgs.Builder builder = GetObjectArgs.builder().bucket(bucket).object(fullPath);

    if (Objects.nonNull(eTag)) {
      builder.notMatchETag(eTag);
    }

    try {
      return Optional.of(minioClient.getObject(builder.build()));
    } catch (Throwable e) {
      return Optional.empty();
    }
  }
}
