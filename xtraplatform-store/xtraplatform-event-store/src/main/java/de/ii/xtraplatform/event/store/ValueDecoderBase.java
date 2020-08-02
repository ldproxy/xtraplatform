package de.ii.xtraplatform.event.store;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.function.Function;

public class ValueDecoderBase<T> implements ValueDecoderMiddleware<T> {

    private final Function<Identifier, T> newInstanceSupplier;
    protected final EventSourcing<T> eventSourcing;// TODO -> ValueCache

    public ValueDecoderBase(Function<Identifier, T> newInstanceSupplier, EventSourcing<T> eventSourcing) {
        this.newInstanceSupplier = newInstanceSupplier;
        this.eventSourcing = eventSourcing;
    }

    @Override
    public T process(Identifier identifier, byte[] payload, ObjectMapper objectMapper, T data) throws IOException {
        T data2 = null;

        if (eventSourcing.isInCache(identifier)) {
            data2 = eventSourcing.getFromCache(identifier);
        } else {
            data2 = newInstanceSupplier.apply(identifier);
        }

        objectMapper.readerForUpdating(data2)
                         .readValue(payload);

        return data2;
    }
}
