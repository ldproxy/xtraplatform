/*
 * Copyright 2025 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.net.URI;
import java.util.List;

public interface WebContext {

  URI getUri();

  default List<String> getPathPrefix() {
    return Strings.isNullOrEmpty(getUri().getPath())
        ? List.of()
        : Splitter.on('/').trimResults().omitEmptyStrings().splitToList(getUri().getPath());
  }
}
