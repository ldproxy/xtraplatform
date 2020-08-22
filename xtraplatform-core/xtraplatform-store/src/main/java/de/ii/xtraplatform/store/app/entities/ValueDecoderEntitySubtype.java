package de.ii.xtraplatform.store.app.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.ValueDecoderMiddleware;
import de.ii.xtraplatform.store.app.EventSourcing;
import de.ii.xtraplatform.store.domain.Identifier;

import java.io.IOException;
import java.util.function.BiFunction;

public class ValueDecoderEntitySubtype implements ValueDecoderMiddleware<EntityData> {

    private final BiFunction<Identifier, String, EntityDataBuilder<EntityData>> newBuilderSupplier;
    private final EventSourcing<EntityData> eventSourcing;// TODO -> ValueCache

    public ValueDecoderEntitySubtype(BiFunction<Identifier, String, EntityDataBuilder<EntityData>> newBuilderSupplier, EventSourcing<EntityData> eventSourcing) {
        this.newBuilderSupplier = newBuilderSupplier;
        this.eventSourcing = eventSourcing;
    }

    @Override
    public EntityData process(Identifier identifier, byte[] payload, ObjectMapper objectMapper, EntityData data) throws IOException {
        if (data.getEntitySubType().isPresent()) {
            EntityDataBuilder<EntityData> builder = newBuilderSupplier.apply(identifier, data.getEntitySubType().get());

            if (eventSourcing.isInCache(identifier)) {
                builder.from(eventSourcing.getFromCache(identifier));
            }

            objectMapper.readerForUpdating(builder)
                        .readValue(payload);

            return builder.build();
        }

        return data;
    }
}
