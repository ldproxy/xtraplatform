package de.ii.xtraplatform.entity.api;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Value {

    //TODO
    @JsonIgnore
    @org.immutables.value.Value.Default
    default long storageVersion() {
        return 1L;
    }
}
