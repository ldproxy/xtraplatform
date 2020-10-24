package de.ii.xtraplatform.store.domain;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(get = "*")
public interface ReloadEvent extends TypedEvent {
    EventFilter filter();
}
