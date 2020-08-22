package de.ii.xtraplatform.store.domain.entities.maptobuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Buildable<T extends Buildable<T>> {

    @JsonIgnore
    BuildableBuilder<T> getBuilder();
}
