package de.ii.xtraplatform.event.store;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface ValueEncoding<T> {

    enum FORMAT {
        UNKNOWN, JSON, YML, YAML, ION;

        static FORMAT fromString(String format) {
            if (Objects.isNull(format)) {
                return UNKNOWN;
            }

            for (FORMAT f: values()) {
                if (Objects.equals(f.name(), format.toUpperCase())) {
                    return f;
                }
            }

            throw new IllegalArgumentException();
        }
    }

    FORMAT getDefaultFormat();

    byte[] serialize(T data);

    byte[] serialize(Map<String, Object> data);

    T deserialize(Identifier identifier, byte[] payload, FORMAT format);

    byte[] nestPayload(byte[] payload, FORMAT format, List<String> nestingPath) throws IOException;

}