package de.ii.xtraplatform.dropwizard.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

public class StoreConfiguration {

    public enum StoreMode {
        READ_WRITE,
        READ_ONLY,
        DISTRIBUTED
    }

    @Valid
    @NotNull
    @JsonProperty
    public StoreMode mode = StoreMode.READ_WRITE;

    @Valid
    @NotNull
    @JsonProperty
    public boolean secured = false;

    @Valid
    @NotEmpty
    @JsonProperty
    public String instancePathPattern = "{type}/{path:**}/{id}";

    @Valid
    @NotNull
    @JsonProperty
    public List<String> overridesPathPatterns = ImmutableList.of("{type}/{path:**}/#overrides#/{id}");

}
