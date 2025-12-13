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
 * @langEn # Jobs
 *     <p>## Options
 *     <p>{@docTable:properties}
 * @langDe # Jobs
 *     <p>## Optionen
 *     <p>{@docTable:properties}
 * @ref:cfgProperties {@link ImmutableJobsConfiguration}
 */
@DocFile(
    path = "application/20-configuration",
    name = "91-jobs.md",
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
@JsonDeserialize(as = ModifiableJobsConfiguration.class)
public interface JobsConfiguration {

  enum QUEUE {
    LOCAL,
    REDIS
  }

  /**
   * @langEn The job queue implementation to use. `LOCAL` uses an in-memory queue and is only
   *     recommended for single node setups. `REDIS` uses a Redis or Valkey server. The server must
   *     be configured in the `redis` section.
   * @langDe Die zu verwendende Job-Queue Implementierung. `LOCAL` verwendet eine In-Memory Queue
   *     und ist nur für Single-Node Setups zu empfehlen. `REDIS` verwendet einen Redis oder Valkey
   *     Server. Der Server muss im Abschnitt `redis` konfiguriert werden.
   * @since v4.6
   * @default LOCAL
   */
  @Value.Default
  default QUEUE getQueue() {
    return QUEUE.LOCAL;
  }

  /**
   * @langEn The maximum number of jobs that are allowed to run concurrently. If requests are to be
   *     answered efficiently at all times, the value should not exceed half of the CPU cores.
   * @langDe Die maximale Anzahl an Jobs, die gleichzeitig ausgeführt werden dürfen. Falls zu jeder
   *     Zeit Requests performant beantwortet können werden sollen, sollte der Wert die Hälfte der
   *     CPU-Kerne nicht überschreiten.
   * @since v4.6
   * @default 1
   */
  @Min(0)
  @Value.Default
  default int getMaxConcurrent() {
    return 1;
  }
}
