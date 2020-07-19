package de.ii.xtraplatform.event.store;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public interface ValueDecoderMiddleware<T> {

    T process(Identifier identifier, byte[] payload, ObjectMapper objectMapper, T data) throws IOException;

    default T recover(Identifier identifier, byte[] payload, ObjectMapper objectMapper) throws IOException {
        throw new IllegalStateException();
    }

    default boolean canRecover() {
        return false;
    }
}
