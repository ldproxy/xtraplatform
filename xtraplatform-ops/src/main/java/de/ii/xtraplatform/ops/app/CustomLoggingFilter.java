/*
 * Copyright 2025 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ops.app;

import static de.ii.xtraplatform.ops.app.FilterUtils.setFilter;

import ch.qos.logback.classic.LoggerContext;
import de.ii.xtraplatform.base.domain.LoggingFilter;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;

public class CustomLoggingFilter {

  private final Map<String, Boolean> flags;
  private final LoggerContext loggerContext;

  @Inject
  public CustomLoggingFilter(Map<String, Boolean> flags, LoggerContext loggerContext) {
    this.flags = flags;
    this.loggerContext = loggerContext;
  }

  public Optional<LoggingFilter> applyFilters() {

    Optional<LoggingFilter> optionalThirdPartyLoggingFilter =
        loggerContext.getTurboFilterList().stream()
            .filter(turboFilter -> turboFilter instanceof LoggingFilter)
            .map(turboFilter -> (LoggingFilter) turboFilter)
            .findFirst();

    optionalThirdPartyLoggingFilter.ifPresent(
        loggingFilter -> {
          flags.forEach((filter, enable) -> setFilter(loggingFilter, filter, enable));
        });

    return optionalThirdPartyLoggingFilter;
  }
}
