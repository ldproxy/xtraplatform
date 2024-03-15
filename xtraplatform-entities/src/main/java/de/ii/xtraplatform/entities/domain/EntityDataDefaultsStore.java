/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import de.ii.xtraplatform.values.domain.Identifier;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface EntityDataDefaultsStore extends MergeableKeyValueStore<Map<String, Object>> {
  String EVENT_TYPE = "defaults";

  Map<String, Object> subtractDefaults(
      Identifier identifier,
      Optional<String> subType,
      Map<String, Object> data,
      List<String> ignoreKeys);

  Map<String, Object> asMap(Identifier identifier, EntityData entityData) throws IOException;

  Optional<Map<String, Object>> getAllDefaults(Identifier identifier, Optional<String> subType);

  EntityDataBuilder<EntityData> getBuilder(Identifier identifier);

  CompletableFuture<Void> onReady();
}
