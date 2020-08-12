package de.ii.xtraplatform.entities.domain.maptobuilder;

public interface Buildable<T extends Buildable<T>> {
    BuildableBuilder<T> getBuilder();
}
