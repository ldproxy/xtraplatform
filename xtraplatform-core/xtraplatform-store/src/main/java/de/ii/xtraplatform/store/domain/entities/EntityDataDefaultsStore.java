/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain.entities;

import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.MergeableKeyValueStore;
import java.util.Map;
import java.util.Optional;

public interface EntityDataDefaultsStore extends MergeableKeyValueStore<Map<String, Object>> {
  String EVENT_TYPE = "defaults";

  Map<String, Object> subtractDefaults(Identifier identifier, Optional<String> subType,
      Map<String, Object> data);

  EntityDataBuilder<EntityData> getBuilder(Identifier identifier);
}
