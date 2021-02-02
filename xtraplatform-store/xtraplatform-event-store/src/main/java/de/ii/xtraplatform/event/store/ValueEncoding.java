package de.ii.xtraplatform.event.store;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public interface ValueEncoding<T> {

    enum FORMAT {
        UNKNOWN(""), JSON("json"), YML("yml"), YAML("yaml")/*, ION*/;

        private final String extension;

        FORMAT(String extension) {
            this.extension = extension;
        }

        static FORMAT fromString(String format) {
            if (Objects.isNull(format)) {
                return UNKNOWN;
            }

            for (FORMAT f: values()) {
                if (Objects.equals(f.extension, format.toLowerCase())) {
                    return f;
                }
            }

            throw new IllegalArgumentException();
        }

        @Override
        public String toString() {
            return extension;
        }
    }

    FORMAT getDefaultFormat();

    byte[] serialize(T data);

    byte[] serialize(Map<String, Object> data);

    T deserialize(Identifier identifier, byte[] payload, FORMAT format);

    byte[] nestPayload(byte[] payload, String format, List<String> nestingPath, Optional<EntityDataDefaults.KeyPathAlias> keyPathAlias) throws IOException;

}