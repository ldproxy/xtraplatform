/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain.entities;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.store.domain.KeyPathAlias;
import java.util.Map;

public interface EntityDataDefaults<T extends EntityData> {

  int getSortPriority();

  EntityDataBuilder<T> getBuilderWithDefaults();

  default Map<String, KeyPathAlias> getAliases() {
    return ImmutableMap.of();
  }
}
