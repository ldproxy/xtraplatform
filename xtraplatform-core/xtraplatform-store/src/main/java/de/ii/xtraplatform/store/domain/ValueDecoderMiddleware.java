package de.ii.xtraplatform.store.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.xtraplatform.store.domain.Identifier;

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
