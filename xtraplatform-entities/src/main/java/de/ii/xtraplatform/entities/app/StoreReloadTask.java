/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.entities.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Splitter;
import de.ii.xtraplatform.base.domain.AppConfiguration;
import de.ii.xtraplatform.entities.domain.EventStore;
import de.ii.xtraplatform.entities.domain.ImmutableEventFilter;
import de.ii.xtraplatform.web.domain.DropwizardPlugin;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.servlets.tasks.Task;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class StoreReloadTask extends Task implements DropwizardPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(StoreReloadTask.class);
  private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private final EventStore eventStore;

  // TODO:  AdminTaskRegistry (OpsPlugin)
  @Inject
  protected StoreReloadTask(EventStore eventStore) {
    super("reload-entities");
    this.eventStore = eventStore;
  }

  @Override
  public void init(AppConfiguration configuration, Environment environment) {
    environment.admin().addTask(this);
  }

  @Override
  public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
    if (LOGGER.isTraceEnabled()) LOGGER.trace("Reload request: {}", parameters);

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
            .addEventTypes("entities")
            .entityTypes(entityTypes)
            .ids(ids)
            .build();

    boolean force = getForce(parameters);

    eventStore.replay(filter, force);
  }

  private List<String> getEntityTypes(Map<String, List<String>> parameters) {
    return getValueList(parameters.get("types"));
  }

  private List<String> getIds(Map<String, List<String>> parameters) {
    return getValueList(parameters.get("ids"));
  }

  private boolean getForce(Map<String, List<String>> parameters) {
    return getValueList(parameters.get("force")).equals(List.of("true"));
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
