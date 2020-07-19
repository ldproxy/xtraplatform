package de.ii.xtraplatform.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import org.hibernate.validator.constraints.NotEmpty;

import javax.annotation.Nullable;
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
    public String location = "store";

    @Valid
    @NotNull
    @JsonProperty
    public boolean secured = false;

    //defaultValuesPathPattern
    @Valid
    @NotNull
    @JsonProperty
    public List<String> defaultValuesPathPatterns = ImmutableList.of(
            "{type}/{path:**}/{id}",
            "{type}/{path:**}/{id}"
    );

    //keyValuePathPattern
    @Valid
    @NotEmpty
    @JsonProperty
    public String instancePathPattern = "{type}/{path:**}/{id}";

    @Valid
    @NotNull
    @JsonProperty
    public List<String> overridesPathPatterns = ImmutableList.of("{type}/{path:**}/#overrides#/{id}");

}
