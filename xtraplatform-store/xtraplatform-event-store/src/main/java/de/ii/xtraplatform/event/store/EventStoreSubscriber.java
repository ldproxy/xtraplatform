package de.ii.xtraplatform.event.store;

public interface EventStoreSubscriber {

    String getEventType();

    void onEmit(Event event);

}
