package de.ii.xtraplatform.event.store;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.function.Function;

public class ValueDecoderBase<T> implements ValueDecoderMiddleware<T> {

    private final Function<Identifier, T> newInstanceSupplier;
    protected final ValueCache<T> valueCache;

    public ValueDecoderBase(Function<Identifier, T> newInstanceSupplier, ValueCache<T> valueCache) {
        this.newInstanceSupplier = newInstanceSupplier;
        this.valueCache = valueCache;
    }

    @Override
    public T process(Identifier identifier, byte[] payload, ObjectMapper objectMapper, T data) throws IOException {
        T data2 = null;

        if (valueCache.isInCache(identifier)) {
            data2 = valueCache.getFromCache(identifier);
        } else {
            data2 = newInstanceSupplier.apply(identifier);
        }

        objectMapper.readerForUpdating(data2)
                         .readValue(payload);

        return data2;
    }
}
