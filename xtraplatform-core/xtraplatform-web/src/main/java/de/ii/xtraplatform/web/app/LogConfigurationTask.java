/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import ch.qos.logback.classic.LoggerContext;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.base.domain.AppConfiguration;
import de.ii.xtraplatform.base.domain.LoggingFilter;
import de.ii.xtraplatform.web.domain.DropwizardPlugin;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.servlets.tasks.Task;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class LogConfigurationTask extends Task implements DropwizardPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(LogConfigurationTask.class);
  private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private final LoggerContext loggerContext;

  @Inject
  protected LogConfigurationTask() {
    super("log-filter");
    this.loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
  }

  @Override
  public void init(AppConfiguration configuration, Environment environment) {
    environment.admin().addTask(this);
  }

  @Override
  public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
    if (LOGGER.isTraceEnabled()) LOGGER.trace("Log filter request: {}", parameters);

    Optional<LoggingFilter> optionalThirdPartyLoggingFilter =
        loggerContext.getTurboFilterList().stream()
            .filter(turboFilter -> turboFilter instanceof LoggingFilter)
            .map(turboFilter -> (LoggingFilter) turboFilter)
            .findFirst();

    optionalThirdPartyLoggingFilter.ifPresent(
        loggingFilter -> {
          getFiltersToEnable(parameters).forEach(filter -> setFilter(loggingFilter, filter, true));
          getFiltersToDisable(parameters)
              .forEach(filter -> setFilter(loggingFilter, filter, false));
        });
  }

  private void setFilter(LoggingFilter loggingFilter, String filter, boolean enable) {
    switch (filter) {
      case "apiRequests":
        loggingFilter.setApiRequests(enable);
        break;
      case "apiRequestUsers":
        loggingFilter.setApiRequestUsers(enable);
        break;
      case "apiRequestHeaders":
        loggingFilter.setApiRequestHeaders(enable);
        break;
      case "apiRequestBodies":
        loggingFilter.setApiRequestBodies(enable);
        break;
      case "sqlQueries":
        loggingFilter.setSqlQueries(enable);
        break;
      case "sqlResults":
        loggingFilter.setSqlResults(enable);
        break;
      case "s3":
        loggingFilter.setS3(enable);
        break;
      case "configDumps":
        loggingFilter.setConfigDumps(enable);
        break;
      case "stackTraces":
        loggingFilter.setStackTraces(enable);
        break;
      case "*":
        loggingFilter.setApiRequests(enable);
        loggingFilter.setApiRequestUsers(enable);
        loggingFilter.setApiRequestHeaders(enable);
        loggingFilter.setApiRequestBodies(enable);
        loggingFilter.setSqlQueries(enable);
        loggingFilter.setSqlResults(enable);
        loggingFilter.setS3(enable);
        loggingFilter.setConfigDumps(enable);
        loggingFilter.setStackTraces(enable);
        break;
    }
  }

  private List<String> getFiltersToEnable(Map<String, List<String>> parameters) {
    return getValueList(parameters.getOrDefault("enable", ImmutableList.of()));
  }

  private List<String> getFiltersToDisable(Map<String, List<String>> parameters) {
    return getValueList(parameters.getOrDefault("disable", ImmutableList.of()));
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
