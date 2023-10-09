/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.infra;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.StoreSource;
import de.ii.xtraplatform.base.domain.StoreSource.Type;
import de.ii.xtraplatform.base.domain.StoreSourceHttpFetcher;
import de.ii.xtraplatform.entities.domain.EntityEvent;
import de.ii.xtraplatform.entities.domain.EventReader;
import de.ii.xtraplatform.entities.domain.EventSource;
import de.ii.xtraplatform.entities.domain.EventStoreDriver;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class EventStoreDriverHttp implements EventStoreDriver {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventStoreDriverHttp.class);

  private final EventReader eventReaderZip;
  private final StoreSourceHttpFetcher httpFetcher;

  @Inject
  EventStoreDriverHttp(AppContext appContext) {
    this.eventReaderZip = new EventReaderZip();
    this.httpFetcher =
        new StoreSourceHttpFetcher(
            appContext.getTmpDir(), appContext.getConfiguration().getHttpClient());
  }

  @Override
  public String getType() {
    return Type.HTTP_KEY;
  }

  @Override
  public boolean isAvailable(StoreSource storeSource) {
    return httpFetcher.isAvailable(storeSource);
  }

  @Override
  public Stream<EntityEvent> load(StoreSource storeSource) {

    if (!storeSource.isArchive()) {
      LOGGER.error("Store source {} only supports archives.", storeSource.getLabel());
      return Stream.empty();
    }

    Optional<Path> cachePath = httpFetcher.load(storeSource);

    if (cachePath.isEmpty()) {
      return Stream.empty();
    }

    EventSource source = from(cachePath.get(), storeSource);

    return source.load(eventReaderZip);
  }

  private EventSource from(Path cachePath, StoreSource source) {
    return new EventSource(cachePath, source, this::adjustPathPattern);
  }

  private String adjustPathPattern(String pattern) {
    return pattern.replaceAll("\\/", "\\" + FileSystems.getDefault().getSeparator());
  }
}
