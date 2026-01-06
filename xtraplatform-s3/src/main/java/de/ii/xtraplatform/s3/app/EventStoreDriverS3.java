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
import de.ii.xtraplatform.base.domain.StoreSourceS3;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.entities.domain.EntityEvent;
import de.ii.xtraplatform.entities.domain.EventReader;
import de.ii.xtraplatform.entities.domain.EventSource;
import de.ii.xtraplatform.entities.domain.EventStoreDriver;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class EventStoreDriverS3 implements EventStoreDriver {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventStoreDriverS3.class);

  @Inject
  public EventStoreDriverS3() {}

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
  public Stream<EntityEvent> load(StoreSource storeSource) {
    if (storeSource instanceof StoreSourceS3) {
      Tuple<MinioClient, String> client = getClient((StoreSourceS3) storeSource);

      EventSource source =
          new EventSource(Path.of(client.second()), storeSource, Function.identity());

      EventReader eventReader = new EventReaderS3(client.first());

      return source.load(eventReader);
    }

    return Stream.empty();
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
