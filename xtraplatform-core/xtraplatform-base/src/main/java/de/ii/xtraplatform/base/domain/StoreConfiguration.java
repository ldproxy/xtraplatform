/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @title Store
 * @en The store contains configuration objects.
 * @de Der Store enthält Konfigurationsobjekte.
 */
public class StoreConfiguration {

  /**
   * @en The store contains configuration objects.
   * @de `READ_WRITE` oder `READ_ONLY`. Bestimmt ob die Software Änderungen am Store vornehmen darf.
   * @default `READ_WRITE`
   */
  public enum StoreMode {
    READ_WRITE,
    READ_ONLY,
    DISTRIBUTED,
  }

  @JsonProperty public StoreMode mode = StoreMode.READ_WRITE;

  @Valid @NotNull @JsonProperty public String location = "store";

  /**
   * @en List of paths with [additional directories](#additional-locations).
   * @de Liste von Pfaden mit [zusätzlichen Verzeichnissnen](#additional-locations).
   * @default `[]`
   */
  @Valid @NotNull @JsonProperty public List<String> additionalLocations = ImmutableList.of();

  @Valid @NotNull @JsonProperty public boolean watch = false;

  @Valid @NotNull @JsonProperty public boolean secured = false;

  @Valid @NotNull @JsonProperty public boolean failOnUnknownProperties = false;

  // defaultValuesPathPattern
  @Valid @NotNull @JsonProperty
  public List<String> defaultValuesPathPatterns =
      ImmutableList.of("{type}/{path:**}/{id}", "{type}/{path:**}/{id}");

  // keyValuePathPattern
  @Valid @NotEmpty @JsonProperty public String instancePathPattern = "{type}/{path:**}/{id}";

  @Valid @NotNull @JsonProperty
  public List<String> overridesPathPatterns = ImmutableList.of("{type}/{path:**}/#overrides#/{id}");
}
