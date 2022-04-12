/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * @title PROJ Coordinate transformations
 */
public class ProjConfiguration {

  /**
   * @en The path to the PROJ directory, either absolute or relative to the data directory.
   * @de Der Pfad zum PROJ-Verzeichnis, entweder absolut oder relativ zum Daten-Verzeichnis.
   * @default `proj`
   */
  @Valid @NotNull @JsonProperty public String location = "proj";
}
