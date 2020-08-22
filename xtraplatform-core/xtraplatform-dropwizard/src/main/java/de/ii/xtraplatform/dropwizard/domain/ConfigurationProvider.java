package de.ii.xtraplatform.dropwizard.domain;

import de.ii.xtraplatform.runtime.domain.XtraPlatformConfiguration;

public interface ConfigurationProvider<T extends XtraPlatformConfiguration> {

    T getConfiguration();
}
