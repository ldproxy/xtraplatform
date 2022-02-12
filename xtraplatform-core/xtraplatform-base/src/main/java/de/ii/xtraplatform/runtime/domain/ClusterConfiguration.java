/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.runtime.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class ClusterConfiguration {
  @Valid @NotNull @JsonProperty public Integer nodeId;

  @Valid
  // @NotEmpty
  @JsonProperty
  public List<Node> peers = ImmutableList.of();

  public static class Node {
    @Valid
    @NotNull
    // @Pattern(regexp = "[\\w-]{3,}")
    @JsonProperty
    public Integer nodeId;

    @Valid
    @Pattern(regexp = "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}")
    @JsonProperty
    public String host;

    @Valid @NotNull @JsonProperty public Integer port = 7081;
  }
  /*public boolean hasNode() {
      return Objects.nonNull(nodes) && !nodes.isEmpty();
  }*/

}
