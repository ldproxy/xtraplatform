package de.ii.xtraplatform.entity.api.maptobuilder;

public interface BuildableBuilder<T extends Buildable<T>> {
    <U extends BuildableBuilder<T>> U from(T value);
    T build();
}
