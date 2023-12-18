/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain;

import com.google.common.hash.Hashing;
import java.util.Optional;
import org.immutables.value.Value;

public interface StaticResourceReader {

  Optional<CachedResource> load(String path, Optional<String> defaultPage);

  @Value.Immutable
  interface CachedResource {

    static CachedResource of(byte[] resource, long lastModified) {
      return ImmutableCachedResource.of(resource, lastModified);
    }

    @Value.Parameter
    byte[] getResource();

    @Value.Parameter
    long getLastModifiedRaw();

    @Value.Derived
    default long getLastModified() {
      long lastModified = getLastModifiedRaw();

      if (lastModified < 1) {
        // Something went wrong trying to get the last modified time: just use the current time
        lastModified = System.currentTimeMillis();
      }

      // zero out the millis since the date we get back from If-Modified-Since will not have them
      lastModified = (lastModified / 1000) * 1000;

      return lastModified;
    }

    @Value.Derived
    default String getETag() {
      return '"' + Hashing.murmur3_128().hashBytes(getResource()).toString() + '"';
    }
  }
}
