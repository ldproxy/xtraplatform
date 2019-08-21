package de.ii.xtraplatform.entity.api.maptobuilder;

public interface ValueBuilder<T extends ValueInstance> {
    <U extends  ValueBuilder<T>> U from(T value);
    T build();
}
