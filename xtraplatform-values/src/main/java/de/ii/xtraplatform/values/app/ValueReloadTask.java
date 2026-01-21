/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.values.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Splitter;
import de.ii.xtraplatform.base.domain.AppConfiguration;
import de.ii.xtraplatform.values.domain.ValueStore;
import de.ii.xtraplatform.web.domain.DropwizardPlugin;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.servlets.tasks.Task;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public class ValueReloadTask extends Task implements DropwizardPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(ValueReloadTask.class);
  private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private final ValueStore valueStore;

  @Inject
  protected ValueReloadTask(ValueStore valueStore) {
    super("reload-values");
    this.valueStore = valueStore;
  }

  @Override
  public void init(AppConfiguration configuration, Environment environment) {
    environment.admin().addTask(this);
  }

  // filter is list of paths, e.g. codelists,maplibre-styles or
  // codelists/foo,maplibre-styles/bar
  @Override
  public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Reload request: {}", parameters);
    }

    List<Path> includes = getPaths(parameters).stream().map(Path::of).collect(Collectors.toList());

    if (valueStore instanceof ValueStoreImpl) {
      ((ValueStoreImpl) valueStore).reload(includes);
    }
  }

  private List<String> getPaths(Map<String, List<String>> parameters) {
    return getValueList(parameters.get("paths"));
  }

  private List<String> getValueList(Collection<String> values) {
    if (Objects.isNull(values)) {
      return List.of();
    }
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
