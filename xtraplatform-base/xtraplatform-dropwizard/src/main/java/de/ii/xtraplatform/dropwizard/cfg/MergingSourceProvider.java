package de.ii.xtraplatform.dropwizard.cfg;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import de.ii.xtraplatform.dropwizard.api.XtraPlatformConfiguration;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class MergingSourceProvider implements ConfigurationSourceProvider {

    private static final String BASE_CFG_FILE = "/cfg.base.yml";

    private final ConfigurationSourceProvider delegate;
    private final List<ByteSource> mergeAfterBase;
    private final ObjectMapper mapper;
    private final ObjectMapper mergeMapper;

    /**
     * Create a new instance.
     *
     * @param delegate The underlying {@link ConfigurationSourceProvider}.
     * @param mergeAfterBase
     */
    public MergingSourceProvider(ConfigurationSourceProvider delegate,
                                 List<ByteSource> mergeAfterBase) {
        this.delegate = requireNonNull(delegate);
        this.mergeAfterBase = mergeAfterBase;

        this.mapper = Jackson.newObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID))
                             .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        this.mergeMapper = mapper.copy()
                                 .setDefaultMergeable(true);
        mergeMapper.configOverride(List.class)
                   .setMergeable(false);
        mergeMapper.configOverride(Map.class)
                   .setMergeable(false);
        mergeMapper.configOverride(Duration.class)
                   .setMergeable(false);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream open(String path) throws IOException {
        try (final InputStream in = delegate.open(path)) {
            //TODO: from constructor param
            ByteSource byteSource = Resources.asByteSource(Resources.getResource(getClass(), BASE_CFG_FILE));

            XtraPlatformConfiguration base = mapper.readValue(byteSource.openStream(), XtraPlatformConfiguration.class);

            //String before = mapper.writeValueAsString(base);

            for (ByteSource byteSource1 : mergeAfterBase) {
                mergeMapper.readerForUpdating(base)
                           .readValue(byteSource1.openStream());
            }

            mergeMapper.readerForUpdating(base)
                       .readValue(in);

            //String merged = mapper.writeValueAsString(base);

            return new ByteArrayInputStream(mapper.writeValueAsBytes(base));
        }
    }
}
