package de.ii.xtraplatform.web.domain;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;

public interface LastModified {

  static Date from(Path path) {
    return Date.from(Instant.ofEpochMilli(path.toFile().lastModified()));
  }
}
