package de.ii.xtraplatform.store.domain;

public interface TypedEvent extends Event {
    String type();
}
