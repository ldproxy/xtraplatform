package de.ii.xtraplatform.event.store;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@Value.Style(get = "*", deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableMutationEvent.Builder.class)
public interface MutationEvent extends Event {

    String type();

    Identifier identifier();

    @Nullable
    byte[] payload();

    @Nullable
    Boolean deleted();
}
