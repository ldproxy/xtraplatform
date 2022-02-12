package de.ii.xtraplatform.base.domain;

import de.ii.xtraplatform.base.domain.Constants.ENV;
import java.net.URI;

public interface AppContext {

  String getName();

  String getVersion();

  ENV getEnvironment();

  AppConfiguration getConfiguration();

  URI getUri();

  default boolean isDevEnv() {
    return getEnvironment() == ENV.DEVELOPMENT;
  }
}
