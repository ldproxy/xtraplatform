package de.ii.xtraplatform.dropwizard.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Objects;

public class ClusterConfiguration {

    @Valid
    @NotNull
    @JsonProperty
    public Integer nodeId;

    @Valid
    //@NotEmpty
    @JsonProperty
    public List<Node> peers = ImmutableList.of();

    static public class Node {

        @Valid
        @NotNull
        //@Pattern(regexp = "[\\w-]{3,}")
        @JsonProperty
        public Integer nodeId;

        @Valid
        //@Pattern(regexp = "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}")
        @JsonProperty
        public String host;

        @Valid
        @NotNull
        @JsonProperty
        public Integer port = 7081;

    }

    /*public boolean hasNode() {
        return Objects.nonNull(nodes) && !nodes.isEmpty();
    }*/

}
