/*
 * Copyright 2025 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain;

import javax.ws.rs.core.MultivaluedMap;

public interface JsonPretty {

  String JSON_PRETTY_HEADER = "x-ldproxy-json-pretty";

  static boolean isJsonPretty(MultivaluedMap<String, Object> httpHeaders) {
    Object headerValue = httpHeaders.getFirst(JSON_PRETTY_HEADER);

    return headerValue instanceof String && "true".equalsIgnoreCase((String) headerValue);
  }

  static void cleanup(MultivaluedMap<String, Object> httpHeaders) {
    httpHeaders.remove(JsonPretty.JSON_PRETTY_HEADER);
  }
}
