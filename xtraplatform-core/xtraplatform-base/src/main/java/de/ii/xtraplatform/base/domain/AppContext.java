/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import de.ii.xtraplatform.base.domain.Constants.ENV;
import java.net.URI;
import java.nio.file.Path;

public interface AppContext {

  String getName();

  String getVersion();

  ENV getEnvironment();

  Path getDataDir();

  Path getTmpDir();

  AppConfiguration getConfiguration();

  URI getUri();

  default boolean isDevEnv() {
    return getEnvironment() == ENV.DEVELOPMENT;
  }
}
