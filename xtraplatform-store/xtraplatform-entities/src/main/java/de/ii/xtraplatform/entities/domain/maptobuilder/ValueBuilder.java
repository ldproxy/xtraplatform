package de.ii.xtraplatform.entities.domain.maptobuilder;

public interface ValueBuilder<T extends ValueInstance> {
    <U extends  ValueBuilder<T>> U from(T value);
    T build();
}
