/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain;

import com.google.common.hash.Funnel;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.SimpleTimeZone;
import javax.ws.rs.core.EntityTag;

@SuppressWarnings("UnstableApiUsage") // com.google.common.hash.*
public interface ETag {

  enum Type {
    WEAK,
    STRONG
  }

  interface Incremental {
    Incremental put(String string);

    Incremental put(byte[] bytes);

    EntityTag build(Type type);
  }

  static EntityTag from(Date date) {
    if (Objects.isNull(date)) {
      return null;
    }

    SimpleDateFormat sdf = new SimpleDateFormat();
    sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
    sdf.applyPattern("dd MMM yyyy HH:mm:ss z");
    String eTag =
        Hashing.murmur3_128().hashString(sdf.format(date), StandardCharsets.UTF_8).toString();

    return new EntityTag(eTag, true);
  }

  static EntityTag from(byte[] byteArray) {
    String eTag = Hashing.murmur3_128().hashBytes(byteArray).toString();

    return new EntityTag(eTag, false);
  }

  static EntityTag from(InputStream inputStream) {
    String eTag = new HashingInputStream(Hashing.murmur3_128(), inputStream).hash().toString();

    return new EntityTag(eTag, false);
  }

  static <S> EntityTag from(S entity, Funnel<S> funnel, String mediaType) {
    String eTag =
        Hashing.murmur3_128()
            .newHasher()
            .putObject(entity, funnel)
            .putString(mediaType, StandardCharsets.UTF_8)
            .hash()
            .toString();

    return new EntityTag(eTag, true);
  }

  static Incremental incremental() {
    Hasher hasher = Hashing.murmur3_128().newHasher();

    return new Incremental() {
      @Override
      public Incremental put(String string) {
        hasher.putString(string, StandardCharsets.UTF_8);
        return this;
      }

      @Override
      public Incremental put(byte[] bytes) {
        hasher.putBytes(bytes);
        return this;
      }

      @Override
      public EntityTag build(Type type) {
        return new EntityTag(hasher.hash().toString(), type == Type.WEAK);
      }
    };
  }
}
