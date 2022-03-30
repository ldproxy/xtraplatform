package de.ii.xtraplatform.store.domain.entities;

import java.util.Optional;

public interface ChangingValue<T> {

  T getValue();

  Optional<ChangingValue<T>> updateWith(ChangingValue<T> delta);
}
