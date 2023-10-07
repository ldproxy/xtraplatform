/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import java.util.concurrent.CompletableFuture;

public interface EventSourcedStore<T> extends EventStoreSubscriber {

  default String getDefaultFormat() {
    return null;
  }

  default CompletableFuture<Void> onStart() {
    return CompletableFuture.completedFuture(null);
  }

  byte[] serialize(T value);

  T deserialize(Identifier identifier, byte[] payload, String format);
}
