/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.stream.Collectors;

@AutoMultiBind
public interface StoreDriver {
  static List<PathMatcher> asMatchers(List<String> globs, String prefix) {
    return globs.stream()
        .map(glob -> FileSystems.getDefault().getPathMatcher("glob:" + prefix + "/" + glob))
        .collect(Collectors.toList());
  }

  String getType();

  boolean isAvailable(StoreSource storeSource);
}
