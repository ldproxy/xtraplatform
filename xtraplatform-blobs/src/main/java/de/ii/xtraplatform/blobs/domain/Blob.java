/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.blobs.domain;

import de.ii.xtraplatform.base.domain.ETag;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;
import javax.ws.rs.core.EntityTag;
import org.immutables.value.Value;

@Value.Immutable
public abstract class Blob {
  @Value.Parameter
  public abstract Path path();

  @Value.Parameter
  public abstract long size();

  @Value.Parameter
  public abstract long lastModified();

  @Value.Parameter
  protected abstract Optional<EntityTag> precomputedETag();

  @Value.Parameter
  protected abstract Optional<String> precomputedContentType();

  @Value.Parameter
  protected abstract Supplier<InputStream> contentSupplier();

  @Value.Lazy
  public byte[] content() {
    try {
      return contentSupplier().get().readAllBytes();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Value.Lazy
  public EntityTag eTag() {
    return precomputedETag().orElseGet(() -> ETag.from(content()));
  }

  @Value.Lazy
  public String contentType() {
    return precomputedContentType()
        .orElseGet(
            () -> {
              // NOTE: URLConnection content-type guessing doesn't seem to work well, maybe try
              // Apache Tika
              String contentType =
                  URLConnection.guessContentTypeFromName(path().getFileName().toString());
              if (contentType == null) {
                try {
                  contentType =
                      URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(content()));
                } catch (IOException e) {
                  // nothing we can do here, just take the default
                }
              }

              if (contentType == null || contentType.isEmpty()) {
                return "application/octet-stream";
              }

              return contentType;
            });
  }

  public Blob withPrecomputedContentType(String value) {
    return ImmutableBlob.builder().from(this).precomputedContentType(value).build();
  }
}
