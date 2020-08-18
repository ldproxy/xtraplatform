package de.ii.xtraplatform.entities.domain.maptobuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Buildable<T extends Buildable<T>> {

    @JsonIgnore
    BuildableBuilder<T> getBuilder();
}
