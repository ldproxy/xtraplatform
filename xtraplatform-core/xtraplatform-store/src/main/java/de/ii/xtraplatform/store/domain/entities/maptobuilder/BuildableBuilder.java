package de.ii.xtraplatform.store.domain.entities.maptobuilder;

public interface BuildableBuilder<T extends Buildable<T>> {
    <U extends BuildableBuilder<T>> U from(T value);
    T build();
}
