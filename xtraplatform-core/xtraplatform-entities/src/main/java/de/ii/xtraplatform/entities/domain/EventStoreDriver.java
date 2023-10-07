/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.xtraplatform.base.domain.StoreDriver;
import de.ii.xtraplatform.base.domain.StoreSource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

@AutoMultiBind
public interface EventStoreDriver extends StoreDriver {

  interface Writer {

    void push(StoreSource storeSource, EntityEvent event) throws IOException;

    void deleteAll(StoreSource storeSource, String type, Identifier identifier, String format)
        throws IOException;
  }

  interface Watcher {
    void listen(StoreSource storeSource, Consumer<List<Path>> watchEventConsumer);
  }

  Stream<EntityEvent> load(StoreSource storeSource);

  default boolean canWrite() {
    return this instanceof Writer;
  }

  default Writer writer() {
    if (!canWrite()) {
      throw new UnsupportedOperationException("Writer not supported");
    }
    return (Writer) this;
  }

  default boolean canWatch() {
    return this instanceof Watcher;
  }

  default Watcher watcher() {
    if (!canWatch()) {
      throw new UnsupportedOperationException("Watcher not supported");
    }
    return (Watcher) this;
  }
}
