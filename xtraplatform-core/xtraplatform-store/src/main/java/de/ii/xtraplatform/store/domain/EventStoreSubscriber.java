package de.ii.xtraplatform.store.domain;

import java.util.List;

public interface EventStoreSubscriber {

    List<String> getEventTypes();

    void onEmit(Event event);

}
