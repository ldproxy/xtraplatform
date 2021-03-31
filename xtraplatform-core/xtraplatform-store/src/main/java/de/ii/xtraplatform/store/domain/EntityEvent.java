package de.ii.xtraplatform.store.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.annotation.Nullable;
import org.immutables.value.Value;

public interface EntityEvent extends TypedEvent, Comparable<EntityEvent> {

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
  default int compareTo(EntityEvent event) {

    int typeCompared = type().compareTo(event.type());

    if (typeCompared != 0) {
      return typeCompared;
    }

    return identifier().compareTo(event.identifier());
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default String asPath() {
    return String.format("%s/%s.%s", type(), identifier().asPath(), format());
  }
}
