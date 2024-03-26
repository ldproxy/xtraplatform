/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import javax.validation.constraints.Min;
import org.immutables.value.Value;

/**
 * @langEn # Background Tasks
 *     <p>## Options
 *     <p>{@docTable:properties}
 * @langDe # Background Tasks
 *     <p>## Optionen
 *     <p>{@docTable:properties}
 */
@DocFile(
    path = "application/20-configuration",
    name = "90-background-tasks.md",
    tables = {
      @DocTable(
          name = "properties",
          rows = {@DocStep(type = Step.JSON_PROPERTIES)},
          columnSet = ColumnSet.JSON_PROPERTIES)
    })
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableBackgroundTasksConfiguration.class)
public interface BackgroundTasksConfiguration {

  /**
   * @en The maximum number of threads available for background processes. If requests are to be
   *     answered efficiently at all times, the value should not exceed half of the CPU cores.
   * @de Die maximale Anzahl an Threads, die für Hintergrundprozesse zur Verfügung stehen. Falls zu
   *     jeder Zeit Requests performant beantwortet können werden sollen, sollte der Wert die Hälfte
   *     der CPU-Kerne nicht überschreiten.
   * @default `1`
   */
  @Min(1)
  @Value.Default
  default int getMaxThreads() {
    return 1;
  }
}
