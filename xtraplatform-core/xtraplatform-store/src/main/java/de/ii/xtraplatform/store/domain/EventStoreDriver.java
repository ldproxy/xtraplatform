package de.ii.xtraplatform.store.domain;

import java.io.IOException;
import java.util.stream.Stream;

public interface EventStoreDriver {

    void start();

    Stream<MutationEvent> loadEventStream();

    void saveEvent(MutationEvent event) throws IOException;

    void deleteAllEvents(String type, Identifier identifier, String format) throws IOException;
}
