/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.domain;

import de.ii.xtraplatform.base.domain.resiliency.Volatile2;
import de.ii.xtraplatform.blobs.domain.BlobStore;
import java.util.concurrent.CompletableFuture;

public interface ValueStore extends Volatile2 {

  <U extends StoredValue> KeyValueStore<U> forTypeWritable(Class<U> type);

  <U extends StoredValue> Values<U> forType(Class<U> type);

  CompletableFuture<Void> onReady();

  BlobStore asBlobStore();
}
