package de.ii.xtraplatform.dropwizard.domain;

import com.google.common.io.ByteSource;
import de.ii.xtraplatform.runtime.domain.Constants;
import de.ii.xtraplatform.runtime.domain.XtraPlatformConfiguration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public interface ApplicationProvider<T extends XtraPlatformConfiguration> {

    Class<T> getConfigurationClass();

    Optional<ByteSource> getConfigurationFileTemplate(String environment);

    Pair<T, Environment> startWithFile(Path configurationFile, Constants.ENV env, Consumer<Bootstrap<T>> initializer);
}
