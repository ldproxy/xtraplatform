package de.ii.xtraplatform.event.store;

import java.util.List;

public interface EventStoreSubscriber {

    List<String> getEventTypes();

    void onEmit(Event event);

}
