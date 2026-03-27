/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.s3.app;

import de.ii.xtraplatform.base.domain.CfgStoreDriver;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.StoreSource;
import de.ii.xtraplatform.base.domain.StoreSourceS3;
import de.ii.xtraplatform.base.domain.util.Tuple;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"PMD.CloseResource"})
public class CfgStoreDriverS3 implements CfgStoreDriver {

  private static final Logger LOGGER = LoggerFactory.getLogger(CfgStoreDriverS3.class);
  static final Path CFG_YML = Path.of("cfg.yml");

  @Override
  public String getType() {
    return "S3";
  }

  @Override
  public boolean isAvailable(StoreSource storeSource) {
    if (storeSource instanceof StoreSourceS3) {
      Tuple<MinioClient, String> client = getClient((StoreSourceS3) storeSource);
      String bucket = client.second();
      try (MinioClient minioClient = client.first()) {
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
      } catch (Throwable e) {
        LogContext.error(LOGGER, e, "S3 Driver");
        return false;
      }
    }
    return false;
  }

  @Override
  public Optional<InputStream> load(StoreSource storeSource) throws IOException {
    if (storeSource instanceof StoreSourceS3) {
      Tuple<MinioClient, String> client = getClient((StoreSourceS3) storeSource);
      String bucket = client.second();

      Path root = Path.of("");
      Path cfg = storeSource.isSingleContent() ? root : root.resolve(CFG_YML);

      try (MinioClient minioClient = client.first()) {
        return Optional.of(
            minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(cfg.toString()).build()));
      } catch (Throwable e) {
        LogContext.error(LOGGER, e, "S3 Driver");
        return Optional.empty();
      }
    }

    return Optional.empty();
  }

  private Tuple<MinioClient, String> getClient(StoreSourceS3 storeSource) {
    String host = storeSource.getSrc().substring(0, storeSource.getSrc().lastIndexOf("/"));
    String bucket = storeSource.getSrc().substring(storeSource.getSrc().lastIndexOf("/") + 1);

    MinioClient minioClient =
        MinioClient.builder()
            .endpoint("https://" + host)
            .credentials(storeSource.getAccessKey(), storeSource.getSecretKey())
            .build();

    return Tuple.of(minioClient, bucket);
  }
}
