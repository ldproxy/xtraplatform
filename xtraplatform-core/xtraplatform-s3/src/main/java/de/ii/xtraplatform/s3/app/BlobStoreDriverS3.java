/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.s3.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.StoreSource;
import de.ii.xtraplatform.base.domain.StoreSource.Content;
import de.ii.xtraplatform.base.domain.StoreSourceS3;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.blobs.domain.BlobCache;
import de.ii.xtraplatform.blobs.domain.BlobSource;
import de.ii.xtraplatform.blobs.domain.BlobStoreDriver;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import java.io.IOException;
import java.nio.file.Path;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class BlobStoreDriverS3 implements BlobStoreDriver {

  private static final Logger LOGGER = LoggerFactory.getLogger(BlobStoreDriverS3.class);

  private final BlobCache cache;

  @Inject
  public BlobStoreDriverS3(BlobCache cache) {
    this.cache = cache;
  }

  @Override
  public String getType() {
    return "S3";
  }

  @Override
  public boolean isAvailable(StoreSource storeSource) {
    if (storeSource instanceof StoreSourceS3) {
      Tuple<MinioClient, String> client = getClient((StoreSourceS3) storeSource);
      String bucket = client.second();

      try {
        return client.first().bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
      } catch (Throwable e) {
        LogContext.error(LOGGER, e, "S3 Driver");
        return false;
      }
    }
    return false;
  }

  @Override
  public BlobSource init(StoreSource storeSource, Content contentType) throws IOException {
    Tuple<MinioClient, String> client = getClient((StoreSourceS3) storeSource);
    String bucket = client.second();
    Path root = Path.of("");

    if (!storeSource.isSingleContent()) {
      root = root.resolve(contentType.getPrefix());
    }

    BlobSource blobSource =
        storeSource.isSingleContent() && storeSource.getPrefix().isPresent()
            ? new BlobSourceS3(
                client.first(), bucket, root, cache, Path.of(storeSource.getPrefix().get()))
            : new BlobSourceS3(client.first(), bucket, root, cache);

    return blobSource;
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
