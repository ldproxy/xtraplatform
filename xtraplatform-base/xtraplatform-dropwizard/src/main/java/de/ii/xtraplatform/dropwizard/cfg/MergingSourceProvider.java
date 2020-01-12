package de.ii.xtraplatform.dropwizard.cfg;

import com.google.common.io.ByteSource;
import de.ii.xtraplatform.configuration.ConfigurationReader;
import de.ii.xtraplatform.runtime.FelixRuntime;
import io.dropwizard.configuration.ConfigurationSourceProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class MergingSourceProvider implements ConfigurationSourceProvider {

    private final ConfigurationSourceProvider delegate;
    private final ConfigurationReader configurationReader;
    private final FelixRuntime.ENV env;

    /**
     * Create a new instance.
     *  @param delegate       The underlying {@link ConfigurationSourceProvider}.
     * @param mergeAfterBase
     * @param env
     */
    public MergingSourceProvider(ConfigurationSourceProvider delegate,
                                 List<ByteSource> mergeAfterBase, FelixRuntime.ENV env) {
        this.delegate = requireNonNull(delegate);
        this.configurationReader = new ConfigurationReader(mergeAfterBase);
        this.env = env;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream open(String path) throws IOException {
        try (final InputStream in = delegate.open(path)) {
            return configurationReader.loadMergedConfig(in, env);
        }
    }
}
