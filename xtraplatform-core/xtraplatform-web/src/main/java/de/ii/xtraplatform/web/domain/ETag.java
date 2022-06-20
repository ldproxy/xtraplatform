package de.ii.xtraplatform.web.domain;

import com.google.common.hash.Funnel;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.SimpleTimeZone;
import javax.ws.rs.core.EntityTag;

@SuppressWarnings("UnstableApiUsage") //com.google.common.hash.*
public interface ETag {

  static EntityTag from(Date date) {
    if (Objects.isNull(date)) {
      return null;
    }

    SimpleDateFormat sdf = new SimpleDateFormat();
    sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
    sdf.applyPattern("dd MMM yyyy HH:mm:ss z");
    String eTag = Hashing.murmur3_128()
        .hashString(sdf.format(date), StandardCharsets.UTF_8)
        .toString();

    return new EntityTag(eTag, true);
  }

  static EntityTag from(byte[] byteArray) {
    String eTag = Hashing.murmur3_128()
        .hashBytes(byteArray)
        .toString();

    return new EntityTag(eTag, false);
  }

  static EntityTag from(File file) {
    String eTag;
    try {
      eTag = Files.asByteSource(file).hash(Hashing.murmur3_128()).toString();
    } catch (IOException e) {
      return null;
    }

    return new EntityTag(eTag, false);
  }

  static EntityTag from(InputStream inputStream) {
    String eTag = new HashingInputStream(Hashing.murmur3_128(), inputStream).hash().toString();

    return new EntityTag(eTag, false);
  }

  static <S> EntityTag from(S entity, Funnel<S> funnel, String mediaType) {
    String eTag = Hashing.murmur3_128()
        .newHasher()
        .putObject(entity, funnel)
        .putString(mediaType, StandardCharsets.UTF_8)
        .hash()
        .toString();

    return new EntityTag(eTag, true);
  }
}
