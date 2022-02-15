package de.ii.xtraplatform.base.domain.util;

import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public interface Tuple<T,U> {

  static <T, U> Tuple<T,U> of(T t, U u) {
    return ImmutableTuple.of(t, u);
  }

  @Nullable
  @Value.Parameter
  T first();

  @Nullable
  @Value.Parameter
  U second();
}
