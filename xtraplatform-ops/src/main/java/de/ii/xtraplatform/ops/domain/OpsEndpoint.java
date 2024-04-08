/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ops.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import java.util.Optional;

@AutoMultiBind
public interface OpsEndpoint {

  default Optional<String> getLabel() {
    return Optional.empty();
  }

  String getEntrypoint();
}
