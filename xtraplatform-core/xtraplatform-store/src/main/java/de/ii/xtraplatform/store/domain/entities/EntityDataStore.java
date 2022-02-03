/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain.entities;

import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.MergeableKeyValueStore;
import de.ii.xtraplatform.store.domain.ValueEncoding;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * id maybe TYPE/ORG/ID, in that case a multitenant middleware would handle splitting into path and
 * id
 *
 * @author zahnen
 */
public interface EntityDataStore<T extends EntityData> extends MergeableKeyValueStore<T> {

  EntityData fromMap(Identifier identifier, Map<String, Object> entityData) throws IOException;

  EntityData fromBytes(Identifier identifier, byte[] entityData)
      throws IOException;

  CompletableFuture<T> patch(String id, Map<String, Object> partialData, String... path);

  ValueEncoding<EntityData> getValueEncoding();

  <U extends T> EntityDataStore<U> forType(Class<U> type);

  Map<String, Object> asMap(Identifier identifier, EntityData entityData) throws IOException;
}
