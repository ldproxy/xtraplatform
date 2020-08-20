package de.ii.xtraplatform.entities.domain.maptobuilder;

public interface BuildableBuilder<T extends Buildable<T>> {
    <U extends BuildableBuilder<T>> U from(T value);
    T build();
}
