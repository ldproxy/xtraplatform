/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.s3.app;

import java.nio.file.Path;
import java.util.Objects;
import javax.annotation.Nullable;

class PathHelper {

  private final Path root;
  @Nullable private final Path prefix;

  PathHelper(Path root, @Nullable Path prefix) {
    this.root = root;
    this.prefix = prefix;
  }

  String full(Path path) {
    return Objects.isNull(prefix)
        ? root.resolve(path).toString()
        : root.resolve(prefix.relativize(path)).toString();
  }

  boolean canHandle(Path path) {
    return Objects.isNull(prefix) || path.startsWith(prefix);
  }
}
