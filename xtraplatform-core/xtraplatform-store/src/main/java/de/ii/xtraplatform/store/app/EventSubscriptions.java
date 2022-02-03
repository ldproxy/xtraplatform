/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app;

import akka.stream.QueueOfferResult;
import de.ii.xtraplatform.store.domain.EventStoreSubscriber;
import de.ii.xtraplatform.store.domain.TypedEvent;
import java.util.concurrent.CompletableFuture;

public interface EventSubscriptions {

  void addSubscriber(EventStoreSubscriber subscriber);

  CompletableFuture<QueueOfferResult> emitEvent(TypedEvent event);

  void startListening();
}
