/*
 * Copyright 2026 interactive instruments GmbH
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
import org.immutables.value.Value;
import org.immutables.value.Value.Default;

/**
 * @langEn # AuditLog
 *     <p>## Options
 *     <p>{@docTable:properties}
 * @langDe # AuditLog
 *     <p>## Optionen
 *     <p>{@docTable:properties}
 * @ref:cfgProperties {@link de.ii.xtraplatform.base.domain.ImmutableAuditLogConfiguration}
 */
@DocFile(
    path = "application/20-configuration",
    name = "120-auditLog.md",
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
@JsonDeserialize(as = ModifiableAuditLogConfiguration.class)
public interface AuditLogConfiguration {

  /**
   * @langEn If `true`, audit logging is enabled; otherwise, it is disabled.
   * @langDe Wenn `true`, wird das Audit-Logging eingeschaltet, ansonsten deaktiviert.
   * @default false
   */
  @Default
  default boolean getEnabled() {
    return false;
  }

  /**
   * @langEn Indicates how often the write process should be retried on errors. Should be set to 0
   *     if no retries are desired.
   * @langDe Gibt an, wie oft der Schreibprozess bei Fehlern wiederholt werden soll. Sollte auf 0
   *     gesetzt werden, falls keine Neuversuche erwünscht sind.
   * @default 3
   */
  @Default
  default int getRetries() {
    return 3;
  }

  /**
   * @langEn Specifies the path to prepend to the log file. `{api}` and `{date}` are replaced with
   *     the API ID and the request's ISO date, respectively. If the request is API-independent,
   *     `{api}` is replaced with "homepage". For example, log files for `{api}/foo/{date}/bar`
   *     could be stored at `resources/logs/audit/vineyards/foo/2026-06-03/bar`.
   * @langDe Gibt den Pfad an, der vor der Log-Datei angehängt werden soll. Dabei werden `{api}` und
   *     `{date}` jeweils mit der API-ID bzw. dem ISO-Datum der Anfrage ersetzt. Falls die Anfrage
   *     API-unabhängig ist, wird `{api}` mit "homepage" ersetzt. Beispielsweise könnten die
   *     Log-Dateien für `{api}/foo/{date}/bar` unter
   *     `resources/logs/audit/vineyards/foo/2026-06-03/bar` gespeichert werden.
   * @default {api}/{date}
   */
  @Default
  default String getPathPrefix() {
    return "{api}/{date}";
  }

  /**
   * @langEn Specifies the format in which logs are stored. Currently supported: `JSON` and
   *     `JSON_PRETTY` (formatted JSON).
   * @langDe Gibt an, in welchem Format die Logs gespeichert werden. Unterstützt werden momentan
   *     `JSON` und `JSON_PRETTY` (formatiertes JSON).
   * @default JSON
   */
  @Default
  default TYPE getType() {
    return TYPE.JSON;
  }

  /**
   * @langEn The `included` list specifies which headers should be logged. The `excluded` list
   *     specifies which headers would be logged according to `included` but should not be logged.
   *     The special value `*` can be used for both lists and covers all headers. If `excluded = [
   *     '*' ]`, no headers are logged.
   * @langDe Die `included`-Liste gibt an, welche Header geloggt werden sollen. Die `excluded`-Liste
   *     gibt an, welche Header gemäß `included` geloggt würden, aber nicht geloggt werden sollen.
   *     Der spezielle Wert `*` kann für beide Listen verwendet werden und umfasst alle Header. Wenn
   *     `excluded = [ '*' ]`, werden keine Header geloggt.
   * @default included: []\nexcluded: []
   */
  @Default
  default HeadersConfiguration getHeaders() {
    return ModifiableHeadersConfiguration.create();
  }

  /**
   * @langEn Specifies which claims from the token should be logged and which should explicitly not
   *     be logged by using `included`/`excluded` lists. The exact logic is the same as in
   *     `headers`.
   * @langDe Gibt mit `included`/`excluded`-Listen an, welche Claims aus dem Token geloggt werden
   *     sollen bzw. explizit nicht geloggt werden dürfen. Die genaue Logik entspricht der in
   *     `headers`.
   * @default included: []\nexcluded: []
   */
  @Default
  default ClaimsConfiguration getClaims() {
    return ModifiableClaimsConfiguration.create();
  }

  /**
   * @langEn Specifies for which HTTP status codes requests should be logged and which should
   *     explicitly not be logged by using `included`/`excluded` lists. The exact logic is the same
   *     as in `headers`.
   * @langDe Gibt mit `included`/`excluded`-Listen an, bei welchen HTTP-Status-Codes Anfragen
   *     geloggt werden sollen bzw. explizit nicht geloggt werden dürfen. Die genaue Logik
   *     entspricht der in `headers`.
   * @default included: []\nexcluded: []
   */
  @Default
  default HttpStatusConfiguration getHttpStatus() {
    return ModifiableHttpStatusConfiguration.create();
  }

  enum TYPE {
    JSON,
    JSON_PRETTY
  }

  // ToDo: Find out how to stop default values from merging with custom values

  @Value.Immutable
  @Value.Modifiable
  @JsonDeserialize(as = ModifiableHeadersConfiguration.class)
  interface HeadersConfiguration {
    @Value.Default
    default List<String> getIncluded() {
      // return List.of("*");
      return List.of();
    }

    @Value.Default
    default List<String> getExcluded() {
      return List.of();
    }
  }

  @Value.Immutable
  @Value.Modifiable
  @JsonDeserialize(as = ModifiableClaimsConfiguration.class)
  interface ClaimsConfiguration {
    @Value.Default
    default List<String> getIncluded() {
      // return List.of("*");
      return List.of();
    }

    @Value.Default
    default List<String> getExcluded() {
      return List.of();
    }
  }

  @Value.Immutable
  @Value.Modifiable
  @JsonDeserialize(as = ModifiableHttpStatusConfiguration.class)
  interface HttpStatusConfiguration {
    @Value.Default
    default List<String> getIncluded() {
      // return List.of("200");
      return List.of();
    }

    @Value.Default
    default List<String> getExcluded() {
      return List.of();
    }
  }
}
