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
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * @langEn # Redis
 *     <p>## Options
 *     <p>{@docTable:properties}
 * @langDe # Redis
 *     <p>## Optionen
 *     <p>{@docTable:properties}
 * @ref:cfgProperties {@link ImmutableRedisConfiguration}
 */
@DocFile(
    path = "application/20-configuration",
    name = "110-redis.md",
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
@JsonDeserialize(as = ModifiableRedisConfiguration.class)
public interface RedisConfiguration {

  /**
   * @langEn The list of Redis or Valkey nodes to connect to, in the format `host:port`.
   * @langDe Die Liste der Redis oder Valkey Knoten, zu denen eine Verbindung hergestellt werden
   *     soll, im Format `host:port`.
   * @since v4.6
   * @default []
   */
  List<String> getNodes();

  /**
   * @langEn Identifies a group of instances that share state (e.g. cache entries) through Redis.
   *     Only instances with the same configuration should use the same value. If not set, falls
   *     back to the instance name.
   * @langDe Identifiziert eine Gruppe von Instanzen, die sich über Redis Zustand (z.B.
   *     Cache-Einträge) teilen. Nur Instanzen mit der gleichen Konfiguration sollten den gleichen
   *     Wert verwenden. Falls nicht gesetzt, wird der Instanzname verwendet.
   * @since v4.8
   * @default optional
   */
  Optional<String> getCluster();
}
