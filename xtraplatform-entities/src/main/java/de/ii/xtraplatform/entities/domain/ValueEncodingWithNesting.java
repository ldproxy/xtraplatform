/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import de.ii.xtraplatform.values.domain.ValueEncoding;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface ValueEncodingWithNesting<T> extends ValueEncoding<T> {

  byte[] nestPayload(
      byte[] payload, String format, List<String> nestingPath, Optional<KeyPathAlias> keyPathAlias)
      throws IOException;
}
