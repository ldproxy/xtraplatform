package de.ii.xtraplatform.event.store;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@Value.Style(get = "*", deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableMutationEvent.Builder.class)
public interface MutationEvent extends Event, Comparable<MutationEvent> {

    String type();

    Identifier identifier();

    @Value.Redacted
    @Nullable
    byte[] payload();

    @Nullable
    Boolean deleted();

    @Nullable
    String format();

    @Override
    default int compareTo(MutationEvent mutationEvent) {

        int typeCompared = type().compareTo(mutationEvent.type());

        if (typeCompared != 0) {
            return typeCompared;
        }

        return identifier().compareTo(mutationEvent.identifier());
    }
}
