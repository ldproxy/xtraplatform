package de.ii.xtraplatform.dropwizard.app;

import de.ii.xtraplatform.dropwizard.domain.AbstractConfigurationProvider;
import de.ii.xtraplatform.runtime.domain.XtraPlatformConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Provides
@Instantiate
public class ConfigurationProviderDefault extends AbstractConfigurationProvider<XtraPlatformConfiguration> {

    @Override
    public Class<XtraPlatformConfiguration> getConfigurationClass() {
        return XtraPlatformConfiguration.class;
    }
}
