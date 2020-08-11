package de.ii.xtraplatform.entities.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

import java.util.Objects;
import java.util.Optional;

public interface AutoEntity {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    Optional<Boolean> getAuto();

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    Optional<Boolean> getAutoPersist();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isAuto() {
        return getAuto().isPresent() && Objects.equals(getAuto().get(), true);
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isAutoPersist() {
        return getAutoPersist().isPresent() && Objects.equals(getAutoPersist().get(), true);
    }

}
