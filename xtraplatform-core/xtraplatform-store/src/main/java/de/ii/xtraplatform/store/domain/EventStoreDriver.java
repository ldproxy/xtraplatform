/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface EventStoreDriver {

  void start();

  Stream<MutationEvent> loadEventStream();

  void saveEvent(MutationEvent event) throws IOException;

  void deleteAllEvents(String type, Identifier identifier, String format) throws IOException;

  default boolean supportsWatch() {
    return false;
  }

  default void startWatching(Consumer<Path> watchEventConsumer) {
    throw new UnsupportedOperationException();
  }
}
