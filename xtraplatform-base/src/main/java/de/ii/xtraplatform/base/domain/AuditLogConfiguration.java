/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;

@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableAuditLogConfiguration.class)
public interface AuditLogConfiguration {

  @Default
  default boolean getEnabled() {
    return false;
  }

  @Default
  default int getRetries() {
    return 3;
  }

  @Default
  default String getPathPrefix() {
    return "{api}/{date}";
  }

  @Default
  default TYPE getType() {
    return TYPE.JSON;
  }

  @Default
  default HeaderConfiguration getHeaders() {
    return ModifiableHeaderConfiguration.create();
  }

  enum TYPE {
    JSON,
    JSON_PRETTY
  }

  @Value.Immutable
  @Value.Modifiable
  @JsonDeserialize(as = ModifiableHeaderConfiguration.class)
  interface HeaderConfiguration {
    @Value.Default
    default List<String> getIncluded() {
      // Find out how to stop default values to merge with custom values
      // return List.of("*");
      return List.of("");
    }

    @Value.Default
    default List<String> getExcluded() {
      return List.of("");
    }
  }
}
