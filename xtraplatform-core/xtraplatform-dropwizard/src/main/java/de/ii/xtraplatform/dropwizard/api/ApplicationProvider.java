package de.ii.xtraplatform.dropwizard.api;

import com.google.common.io.ByteSource;
import de.ii.xtraplatform.runtime.FelixRuntime;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public interface ApplicationProvider<T extends XtraPlatformConfiguration> {

    Class<T> getConfigurationClass();

    Optional<ByteSource> getConfigurationFileTemplate(String environment);

    Pair<T, Environment> startWithFile(Path configurationFile, FelixRuntime.ENV env, Consumer<Bootstrap<T>> initializer);
}
