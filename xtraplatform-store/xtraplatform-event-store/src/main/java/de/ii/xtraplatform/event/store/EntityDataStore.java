/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.event.store;

import de.ii.xtraplatform.entities.domain.EntityData;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * id maybe TYPE/ORG/ID, in that case a multitenant middleware would handle splitting into path and id
 *
 * @author zahnen
 */
public interface EntityDataStore<T extends EntityData> extends MergeableKeyValueStore<T> {

    CompletableFuture<T> patch(String id, Map<String, Object> partialData, String... path);

    <U extends T> EntityDataStore<U> forType(Class<U> type);

}