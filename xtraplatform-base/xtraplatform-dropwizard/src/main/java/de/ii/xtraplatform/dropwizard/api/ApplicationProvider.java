package de.ii.xtraplatform.dropwizard.api;

import com.google.common.io.ByteSource;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public interface ApplicationProvider<T extends XtraPlatformConfiguration> {

    Class<T> getConfigurationClass();

    Optional<ByteSource> getConfigurationFileTemplate(String environment);

    Pair<T, Environment> startWithFile(Path configurationFile, Consumer<Bootstrap<T>> initializer);
}
