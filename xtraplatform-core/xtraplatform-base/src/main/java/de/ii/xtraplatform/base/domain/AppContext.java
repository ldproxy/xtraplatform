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

  Path getConfigurationFile();

  AppConfiguration getConfiguration();

  URI getUri();

  default boolean isDevEnv() {
    return getEnvironment() == ENV.DEVELOPMENT;
  }
}
