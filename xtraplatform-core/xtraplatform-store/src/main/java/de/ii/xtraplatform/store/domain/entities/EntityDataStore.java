/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain.entities;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.ImmutableIdentifier;
import de.ii.xtraplatform.store.domain.MergeableKeyValueStore;
import de.ii.xtraplatform.store.domain.ValueEncoding;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * id maybe TYPE/ORG/ID, in that case a multitenant middleware would handle splitting into path and
 * id
 *
 * @author zahnen
 */
public interface EntityDataStore<T extends EntityData> extends MergeableKeyValueStore<T> {

  String EVENT_TYPE_ENTITIES = "entities";
  String EVENT_TYPE_OVERRIDES = "overrides";
  List<String> EVENT_TYPES = ImmutableList.of(EVENT_TYPE_ENTITIES, EVENT_TYPE_OVERRIDES);

  static String entityType(Identifier identifier) {
    if (identifier.path().isEmpty()) {
      throw new IllegalArgumentException("Invalid path, no entity type found.");
    }
    return identifier.path().get(identifier.path().size() - 1);
  }

  static List<String> entityGroup(Identifier identifier) {
    return identifier.path().size() > 1
        ? Lists.reverse(identifier.path().subList(0, identifier.path().size() - 1))
        : List.of();
  }

  static Identifier defaults(Identifier identifier, String subType) {
    return ImmutableIdentifier.builder()
        .id(EntityDataDefaultsStore.EVENT_TYPE)
        .path(entityGroup(identifier))
        .addPath(entityType(identifier))
        .addPath(subType.toLowerCase())
        .build();
  }

  static Identifier defaults(Identifier identifier, Optional<String> subType) {
    if (subType.isPresent()) {
      return defaults(identifier, subType.get());
    }

    return ImmutableIdentifier.builder()
        .id(EntityDataDefaultsStore.EVENT_TYPE)
        .path(entityGroup(identifier))
        .addPath(entityType(identifier))
        .build();
  }

  EntityData fromMap(Identifier identifier, Map<String, Object> entityData) throws IOException;

  EntityData fromBytes(Identifier identifier, byte[] entityData) throws IOException;

  CompletableFuture<T> patch(String id, Map<String, Object> partialData, String... path);

  CompletableFuture<T> patch(
      String id, Map<String, Object> partialData, boolean skipLastModified, String... path);

  ValueEncoding<EntityData> getValueEncoding();

  EntityDataBuilder<EntityData> getBuilder(Identifier identifier, Optional<String> entitySubtype);

  <U extends T> EntityDataStore<U> forType(Class<U> type);

  Map<String, Object> asMap(Identifier identifier, EntityData entityData) throws IOException;
}
