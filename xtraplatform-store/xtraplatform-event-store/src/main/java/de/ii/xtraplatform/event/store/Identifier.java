package de.ii.xtraplatform.event.store;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Value.Style(get = "*")
@JsonDeserialize(as = ImmutableIdentifier.class)
public interface Identifier {

    String id();

    List<String> path();

    static Identifier from(String id, String... path) {

        return ImmutableIdentifier.builder()
                                  .id(id)
                                  .addPath(path)
                                  .build();
    }
}
