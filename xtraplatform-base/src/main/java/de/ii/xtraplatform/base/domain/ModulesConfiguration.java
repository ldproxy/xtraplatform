/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import org.immutables.value.Value;

/**
 * @langEn # Modules
 *     <p>## Options
 *     <p>{@docTable:properties}
 * @langDe # Modules
 *     <p>## Optionen
 *     <p>{@docTable:properties}
 * @ref:cfgProperties {@link de.ii.xtraplatform.base.domain.ImmutableModulesConfiguration}
 */
@DocFile(
    path = "application/20-configuration",
    name = "80-modules.md",
    tables = {
      @DocTable(
          name = "properties",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:cfgProperties}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES)
    })
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableModulesConfiguration.class)
public interface ModulesConfiguration {

  enum Startup {
    ASYNC,
    SYNC
  }

  enum Maturity {
    PROPOSAL,
    CANDIDATE,
    MATURE
  }

  enum Maintenance {
    NONE,
    LOW,
    FULL
  }

  /**
   * @langEn When `ASYNC` modules will start in parallel and dependencies are resolved event-driven.
   *     That also means the startup will not fail on recoverable errors, e.g. when an external
   *     resource is missing but might arrive later. When `SYNC`, startup behaves like in v3.x, i.e.
   *     modules will start sequentially in a fixed order and any errors will abort the startup.
   * @langDe Wenn `ASYNC` werden die Module parallel gestartet und die Auflösung von Abhängigkeiten
   *     passiert ereignisgesteuert. D.h. auch, dass der Start bei behebbaren Fehlern nicht
   *     fehlschlägt, z.B. wenn eine externe Ressource fehlt aber später verfügbar sein könnte. Wenn
   *     `SYNC` verhält sich der Start wie in v3.x, die Module werden sequentiell in einer festen
   *     Reihenfolge gestartet und bei Fehlern wird der Start abgebrochen.
   * @since v4.0
   * @default ASYNC
   */
  @Value.Default
  default Startup getStartup() {
    return Startup.ASYNC;
  }

  @JsonIgnore
  @Value.Derived
  default boolean isStartupAsync() {
    return getStartup() == Startup.ASYNC;
  }

  /**
   * @langEn The minimum maturity of modules that should be loaded, all other modules will be
   *     ignored. Possible values are `PROPOSAL`, `CANDIDATE` and `MATURE`. See [Module
   *     Lifecycle](../../references/modules.md) for a list of all modules and their
   *     classifications.
   * @langDe Die minimale Maturity von Modulen die geladen werden sollen, all anderen Module werden
   *     ignoriert. Möglich Werte sind `PROPOSAL`, `CANDIDATE` und `MATURE`. Siehe
   *     [Modul-Lebenszyklus](../../references/modules.md) für eine Liste aller Module und ihrer
   *     Klassifikationen.
   * @since v4.0
   * @default CANDIDATE
   */
  @Value.Default
  default Maturity getMinMaturity() {
    return Maturity.CANDIDATE;
  }

  /**
   * @langEn The minimum maintenance level of modules that should be loaded, all other modules will
   *     be ignored. Possible values are `NONE`, `LOW` and `FULL`. See [Module
   *     Lifecycle](../../references/modules.md) for a list of all modules and their
   *     classifications.
   * @langDe Der minimale Maintenance-Level von Modulen die geladen werden sollen, all anderen
   *     Module werden ignoriert. Möglich Werte sind `NONE`, `LOW` und `FULL`. Siehe
   *     [Modul-Lebenszyklus](../../references/modules.md) für eine Liste aller Module und ihrer
   *     Klassifikationen.
   * @since v4.0
   * @default LOW
   */
  @Value.Default
  default Maintenance getMinMaintenance() {
    return Maintenance.LOW;
  }
}
