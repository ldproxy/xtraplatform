package de.ii.xtraplatform.event.store;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(get = "*")
public interface StateChangeEvent extends Event {
    enum STATE {REPLAYING, LISTENING}

    STATE state();

    String type();
}
