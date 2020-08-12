package de.ii.xtraplatform.store.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xtraplatform.store.domain.Builder;
import de.ii.xtraplatform.store.domain.Value;
import de.ii.xtraplatform.store.domain.ValueDecoderMiddleware;
import de.ii.xtraplatform.event.store.EventSourcing;
import de.ii.xtraplatform.store.domain.Identifier;

import java.io.IOException;
import java.util.function.Function;

public class ValueDecoderWithBuilder<T extends Value> implements ValueDecoderMiddleware<T> {

    private final Function<Identifier, Builder<T>> newBuilderSupplier;
    private final EventSourcing<T> eventSourcing;// TODO -> ValueCache

    public ValueDecoderWithBuilder(Function<Identifier, Builder<T>> newBuilderSupplier, EventSourcing<T> eventSourcing) {
        this.newBuilderSupplier = newBuilderSupplier;
        this.eventSourcing = eventSourcing;
    }

    @Override
    public T process(Identifier identifier, byte[] payload, ObjectMapper objectMapper, T data) throws IOException {
        Builder<T> builder = newBuilderSupplier.apply(identifier);

        if (eventSourcing.isInCache(identifier)) {
            builder.from(eventSourcing.getFromCache(identifier));
        }

        objectMapper.readerForUpdating(builder)
                    .readValue(payload);

        return builder.build();
    }
}
