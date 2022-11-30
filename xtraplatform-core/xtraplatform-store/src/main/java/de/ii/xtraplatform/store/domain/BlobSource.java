/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain;

import java.nio.file.Path;

public interface BlobSource extends BlobReader {

  boolean canHandle(Path path);

  default boolean canWrite() {
    return this instanceof BlobWriter;
  }

  default BlobWriter writer() {
    if (!canWrite()) {
      throw new UnsupportedOperationException("Writer not supported");
    }
    return (BlobWriter) this;
  }

  default boolean canLocals() {
    return this instanceof BlobLocals;
  }

  default BlobLocals local() {
    if (!canLocals()) {
      throw new UnsupportedOperationException("Locals not supported");
    }
    return (BlobLocals) this;
  }
}
