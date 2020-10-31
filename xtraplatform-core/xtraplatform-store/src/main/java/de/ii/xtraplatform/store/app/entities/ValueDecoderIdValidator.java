package de.ii.xtraplatform.store.app.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.ValueDecoderMiddleware;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

public class ValueDecoderIdValidator implements ValueDecoderMiddleware<EntityData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValueDecoderIdValidator.class);

    @Override
    public EntityData process(Identifier identifier, byte[] payload, ObjectMapper objectMapper, EntityData data) throws IOException {

        if (!Objects.equals(identifier.id(), data.getId())) {
            LOGGER.error("Id mismatch: ignored entity '{}' because 'id' is set to '{}'", identifier.asPath(), data.getId());
            return null;
        }

        return data;
    }
}
