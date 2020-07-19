package de.ii.xtraplatform.entity.api;

public interface Value {

    //TODO
    @org.immutables.value.Value.Default
    default long storageVersion() {
        return 1L;
    }
}
