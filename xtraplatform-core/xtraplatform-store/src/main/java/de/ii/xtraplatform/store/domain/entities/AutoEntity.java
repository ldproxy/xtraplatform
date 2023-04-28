/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

public interface AutoEntity {

  /**
   * @langEn Option to generate missing definitions automatically from the data source.
   * @langDe Steuert ob fehlende Definitionen beim Start automatisch aus der Datenquelle bestimmt
   *     werden sollen (Auto-Modus).
   * @default false
   */
  @JsonProperty(value = "auto", access = JsonProperty.Access.WRITE_ONLY)
  Optional<Boolean> getAuto();

  /**
   * @langEn Option to persist definitions generated with `auto` to the configuration file. The
   *     [Store](/application/40-store.md) must not be `READ_ONLY` for this to take effect.
   * @langDe Steuert ob mit `auto` generierte Definitionen in die Konfigurationsdatei geschrieben
   *     werden sollen. Setzt voraus, dass der [Store](/de/application/40-store.md) nicht
   *     `READ_ONLY` ist.
   * @default false
   */
  @JsonProperty(value = "autoPersist", access = JsonProperty.Access.WRITE_ONLY)
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
