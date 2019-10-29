package de.ii.xtraplatform.dropwizard.api;

public interface ConfigurationProvider<T extends XtraPlatformConfiguration> {

    T getConfiguration();
}
