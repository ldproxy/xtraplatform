package de.ii.xtraplatform.store.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Value.Style(get = "*")
@JsonDeserialize(as = ImmutableIdentifier.class)
public interface Identifier extends Comparable<Identifier> {

    String id();

    List<String> path();

    static Identifier from(String id, String... path) {

        return ImmutableIdentifier.builder()
                                  .id(id)
                                  .addPath(path)
                                  .build();
    }

    @Override
    default int compareTo(Identifier identifier) {

        for (int i = 0; i < path().size() && i < identifier.path().size(); i++) {
            int compared = path().get(i)
                           .compareTo(identifier.path()
                                                .get(i));
            if (compared != 0) {
                return compared;
            }
        }

        int lengthDiff = path().size() - identifier.path().size();

        if (lengthDiff != 0) {
            return lengthDiff;
        }

        return id().compareTo(identifier.id());
    }
}
