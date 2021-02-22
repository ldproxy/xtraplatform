/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMultimap;
import de.ii.xtraplatform.dropwizard.domain.Dropwizard;
import de.ii.xtraplatform.store.domain.EventStore;
import de.ii.xtraplatform.store.domain.ImmutableEventFilter;
import io.dropwizard.servlets.tasks.Task;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author zahnen */
@Component
@Instantiate
public class StoreReloadTask extends Task {

  private static final Logger LOGGER = LoggerFactory.getLogger(StoreReloadTask.class);
  private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private final EventStore eventStore;

  protected StoreReloadTask(@Requires Dropwizard dropwizard, @Requires EventStore eventStore) {
    super("reload-entities");
    this.eventStore = eventStore;

    dropwizard.getEnvironment().admin().addTask(this);
  }

  @Override
  public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output)
      throws Exception {
    LOGGER.debug("RELOAD {}", parameters);

    List<String> entityTypes = getEntityTypes(parameters);

    if (entityTypes.isEmpty()) {
      output.println("No entity type given");
      output.flush();
      return;
    }
    List<String> ids = getIds(parameters);

    if (ids.isEmpty()) {
      output.println("No id given");
      output.flush();
      return;
    }

    ImmutableEventFilter filter =
        ImmutableEventFilter.builder()
            .eventType("entities")
            .entityTypes(entityTypes)
            .ids(ids)
            .build();

    eventStore.replay(filter);
  }

  private List<String> getEntityTypes(ImmutableMultimap<String, String> parameters) {
    return getValueList(parameters.get("types"));
  }

  private List<String> getIds(ImmutableMultimap<String, String> parameters) {
    return getValueList(parameters.get("ids"));
  }

  private List<String> getValueList(Collection<String> values) {
    return values.stream()
        .flatMap(
            value -> {
              if (value.contains(",")) {
                return SPLITTER.splitToList(value).stream();
              }
              return Stream.of(value);
            })
        .collect(Collectors.toList());
  }
}
