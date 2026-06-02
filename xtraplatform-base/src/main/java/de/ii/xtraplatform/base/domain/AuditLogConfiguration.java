/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableAuditLogConfiguration.class)
public interface AuditLogConfiguration {

  @Value.Default
  default boolean getEnabled() {
    return false;
  }

  @Value.Default
  default int getRetries() {
    return 3;
  }

  @Value.Default
  default String getPathPrefix() {
    return "{api}/{date}";
  }
}
