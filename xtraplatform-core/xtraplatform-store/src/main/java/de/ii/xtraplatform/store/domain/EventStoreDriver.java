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
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface EventStoreDriver {

  interface Write {
    void push(EntityEvent event) throws IOException;

    void deleteAll(String type, Identifier identifier, String format) throws IOException;
  }

  interface Watch {
    void start(Consumer<List<Path>> watchEventConsumer);
  }

  String getType();

  void start();

  Stream<EntityEvent> loadEventStream();

  default boolean canWrite() {
    return this instanceof Write;
  }

  default Write write() {
    if (!canWrite()) {
      throw new UnsupportedOperationException("Write not supported");
    }
    return (Write) this;
  }

  default boolean canWatch() {
    return this instanceof Watch;
  }

  default Watch watch() {
    if (!canWatch()) {
      throw new UnsupportedOperationException("Watch not supported");
    }
    return (Watch) this;
  }
}
