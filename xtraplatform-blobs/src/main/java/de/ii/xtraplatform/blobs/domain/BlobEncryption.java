/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.blobs.domain;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

public interface BlobEncryption {

  Optional<InputStream> contentEncrypted(Path path) throws IOException;

  Optional<Blob> getEncrypted(Path path) throws IOException;

  void putEncrypted(Path path, InputStream content) throws IOException;
}
