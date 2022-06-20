/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;

public interface LastModified {

  static Date from(Path path) {
    return Date.from(Instant.ofEpochMilli(path.toFile().lastModified()));
  }
}
