package de.ii.xtraplatform.store.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xtraplatform.store.domain.ValueDecoderMiddleware;
import de.ii.xtraplatform.store.domain.Identifier;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import org.apache.commons.text.StrSubstitutor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ValueDecoderEnvVarSubstitution implements ValueDecoderMiddleware<byte[]> {

    private final StrSubstitutor substitutor;

    public ValueDecoderEnvVarSubstitution() {
        this.substitutor = new EnvironmentVariableSubstitutor(false, true);
    }

    @Override
    public byte[] process(Identifier identifier, byte[] payload, ObjectMapper objectMapper, byte[] data) throws IOException {

        final String config = new String(payload, StandardCharsets.UTF_8);
        final String substituted = substitutor.replace(config);

        return substituted.getBytes(StandardCharsets.UTF_8);
    }
}
