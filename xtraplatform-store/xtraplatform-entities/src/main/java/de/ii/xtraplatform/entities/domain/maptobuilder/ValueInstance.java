package de.ii.xtraplatform.entities.domain.maptobuilder;

public interface ValueInstance {
    <U extends ValueBuilder<? extends ValueInstance>> U toBuilder();
}
