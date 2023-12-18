/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

/**
 * @title Manager
 */
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableManagerConfiguration.class)
public interface ManagerConfiguration {

  /**
   * @en Should the Manager app be enabled to manage the configuration (see
   *     [Manager](README.md#manager))?
   * @de Soll die Manager-App zur Verwaltung der Konfiguration aktiviert werden (siehe
   *     [Manager](README.md#manager))?
   * @default `true`
   */
  @Value.Default
  default boolean getEnabled() {
    return true;
  }
}
