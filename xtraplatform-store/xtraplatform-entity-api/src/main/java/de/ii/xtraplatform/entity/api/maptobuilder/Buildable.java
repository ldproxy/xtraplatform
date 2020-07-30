package de.ii.xtraplatform.entity.api.maptobuilder;

public interface Buildable<T extends Buildable<T>> {
    BuildableBuilder<T> getBuilder();
}
