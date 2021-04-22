package de.ii.xtraplatform.manager.app;

import de.ii.xtraplatform.store.domain.entities.EntityState;
import de.ii.xtraplatform.streams.domain.Event;
import java.util.function.Consumer;
import org.immutables.value.Value;

@Value.Immutable
public interface EntityStateEvent extends Event, EntityState {

  @Override
  default void addListener(
      Consumer<EntityState> listener) {

  }
}
