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
