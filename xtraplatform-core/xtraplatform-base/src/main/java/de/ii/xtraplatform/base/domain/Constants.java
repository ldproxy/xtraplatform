/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

public class Constants {
  public static final String APPLICATION_KEY = "de.ii.xtraplatform.application.name";
  public static final String VERSION_KEY = "de.ii.xtraplatform.application.version";
  public static final String DATA_DIR_KEY = "de.ii.xtraplatform.directories.data";
  public static final String ENV_KEY = "de.ii.xtraplatform.environment";
  public static final String USER_CONFIG_PATH_KEY = "de.ii.xtraplatform.userConfigPath";
  public static final String TMP_DIR_PROP = "java.io.tmpdir";

  public enum ENV {
    PRODUCTION,
    DEVELOPMENT,
    CONTAINER,
  }
}
