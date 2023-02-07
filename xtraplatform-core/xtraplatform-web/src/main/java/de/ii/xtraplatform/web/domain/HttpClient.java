/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain;

import de.ii.xtraplatform.streams.domain.Reactive;
import java.io.InputStream;
import java.util.Map;
import javax.ws.rs.core.MediaType;

public interface HttpClient {
  Reactive.Source<byte[]> get(String url);

  InputStream getAsInputStream(String url);

  InputStream getAsInputStream(String url, Map<String, String> headers);

  InputStream postAsInputStream(
      String url, byte[] body, MediaType mediaType, Map<String, String> headers);
}
