/*
 * Copyright 2025 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ops.app;

import de.ii.xtraplatform.base.domain.LoggingFilter;

public class FilterUtils {

  public static void setFilter(LoggingFilter loggingFilter, String filter, boolean enable) {
    System.out.println("Setting filter: ");
    System.out.println(filter + enable);

    switch (filter) {
      case "apiRequests":
        loggingFilter.setApiRequests(enable);
        break;
      case "showThirdPartyLoggers":
        loggingFilter.setShowThirdPartyLoggers(enable);
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
      case "wiring":
        loggingFilter.setWiring(enable);
        break;
      case "jobs":
        loggingFilter.setJobs(enable);
        break;
      default:
        throw new IllegalArgumentException("Unknown filter: " + filter);
    }
  }
}
