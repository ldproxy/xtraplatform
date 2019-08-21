package de.ii.xtraplatform.entity.api.maptobuilder;

public interface ValueInstance {
    <U extends ValueBuilder<? extends ValueInstance>> U toBuilder();
}
