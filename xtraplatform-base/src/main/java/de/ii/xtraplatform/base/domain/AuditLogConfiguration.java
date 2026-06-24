/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.OptBoolean;
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
 *     <p>## Storage
 *     <p>The log entries are stored in the resource store in the `logs/audit` directory. The file
 *     name matches the request id from the application log.
 *     <p>## Example
 * @langDe # AuditLog
 *     <p>## Optionen
 *     <p>{@docTable:properties}
 *     <p>## Speicherung
 *     <p>Die Log-Einträge werden im Ressourcen-Store im Verzeichnis `logs/audit` abgelegt. Der
 *     Dateiname entspricht der Request-ID aus dem Anwendungsprotokoll.
 *     <p>## Beispiel
 * @langAll <code>
 * ```yml
 * auditLog:
 *   enabled: true
 *   retries: 3
 *   type: JSON_PRETTY
 *   pathPrefix: "mysubdirectory/{api}/{date}"
 *   headers:
 *     included: [ "*" ]
 *     excluded: [ "Accept" ]
 *   claims:
 *     included: [ "*" ]
 *     excluded: [ ]
 *   httpStatus:
 *     included: [ "200" ]
 *     excluded: [ ]
 * ```
 *     </code>
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
   * @langEn If `true`, audit logging is enabled for all APIs. APIs can be explicitly disabled in
   *     the [API configuration](../../services/#audit-logging). Audit logging is globally disabled
   *     if `false`.
   * @langDe Wenn `true`, wird das Audit-Logging für alle APIs eingeschaltet. Einzelne APIs können
   *     in der [API-Konfiguration](../../services/#audit-logging) explizit deaktiviert werden.
   *     Audit-Logging ist global deaktiviert, wenn `false`.
   * @default false
   * @since 4.8
   */
  @Default
  default boolean getEnabled() {
    return false;
  }

  /**
   * @langEn Indicates how often the write process should be retried on errors. Should be set to `0`
   *     if no retries are desired. If writing fails after the specified number of retries, the log
   *     entry will be written to the application log.
   * @langDe Gibt an, wie oft der Schreibprozess bei Fehlern wiederholt werden soll. Sollte auf `0`
   *     gesetzt werden, falls keine Wiederholungen erwünscht sind. Wenn das Schreiben nach der
   *     angegebenen Anzahl von Wiederholungen fehlschlägt, wird der Log-Eintrag ins
   *     Anwendungsprotokoll geschrieben.
   * @default 3
   * @since 4.8
   */
  @Default
  default int getRetries() {
    return 3;
  }

  /**
   * @langEn Specifies the path to prepend to the log file. `{api}` and `{date}` are replaced with
   *     the API ID and the request's ISO date, respectively. For example, log files for
   *     `{api}/foo/{date}/bar` would be stored at
   *     `resources/logs/audit/vineyards/foo/2026-06-03/bar`.
   * @langDe Gibt den Pfad an, der der Log-Datei vorangestellt werden soll. Dabei werden `{api}` und
   *     `{date}` jeweils mit der API-ID bzw. dem ISO-Datum der Anfrage ersetzt. Beispielsweise
   *     könnten die Log-Dateien für `{api}/foo/{date}/bar` unter
   *     `resources/logs/audit/vineyards/foo/2026-06-03/bar` gespeichert werden.
   * @default {api}/{date}
   * @since 4.8
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
   * @since 4.8
   */
  @Default
  default TYPE getType() {
    return TYPE.JSON;
  }

  /**
   * @langEn The `included` list specifies which headers should be logged. The `excluded` list
   *     specifies which headers from `included` should not be logged. The special value `*` can be
   *     used for both lists and covers all headers. If `excluded: [ '*' ]`, no headers are logged.
   * @langDe Die `included`-Liste gibt an, welche Header geloggt werden sollen. Die `excluded`-Liste
   *     gibt an welche Header aus `included` nicht geloggt werden sollen. Der spezielle Wert `*`
   *     kann für beide Listen verwendet werden und umfasst alle Header. Wenn `excluded: [ '*' ]`,
   *     werden keine Header geloggt.
   * @default included: [ '*' ], excluded: []
   * @since 4.8
   */
  @Default
  default HeadersConfiguration getHeaders() {
    return ModifiableHeadersConfiguration.create();
  }

  /**
   * @langEn Specifies which claims from the token should be logged and which should explicitly not
   *     be logged. Uses the same `included`/`excluded` logic as `headers`.
   * @langDe Gibt an, welche Claims aus dem Token geloggt werden sollen und welche explizit nicht
   *     geloggt werden sollen. Verwendet die gleiche `included`/`excluded`-Logik wie `headers`.
   * @default included: [], excluded: []
   * @since 4.8
   */
  @Default
  default ClaimsConfiguration getClaims() {
    return ModifiableClaimsConfiguration.create();
  }

  /**
   * @langEn Specifies for which HTTP status codes requests should be logged and which should
   *     explicitly not be logged. Uses the same `included`/`excluded` logic as `headers`.
   * @langDe Gibt an, für welche HTTP-Statuscodes Anfragen geloggt werden sollen und welche explizit
   *     nicht geloggt werden sollen. Verwendet die gleiche `included`/`excluded`-Logik wie
   *     `headers`.
   * @default included: [ '200' ], excluded: []
   * @since 4.8
   */
  @Default
  default HttpStatusConfiguration getHttpStatus() {
    return ModifiableHttpStatusConfiguration.create();
  }

  enum TYPE {
    JSON,
    JSON_PRETTY
  }

  @Value.Immutable
  @Value.Modifiable
  @JsonDeserialize(as = ModifiableHeadersConfiguration.class)
  interface HeadersConfiguration {
    @Value.Default
    @JsonMerge(OptBoolean.FALSE)
    default List<String> getIncluded() {
      return List.of("*");
    }

    @Value.Default
    @JsonMerge(OptBoolean.FALSE)
    default List<String> getExcluded() {
      return List.of();
    }
  }

  @Value.Immutable
  @Value.Modifiable
  @JsonDeserialize(as = ModifiableClaimsConfiguration.class)
  interface ClaimsConfiguration {
    @Value.Default
    @JsonMerge(OptBoolean.FALSE)
    default List<String> getIncluded() {
      return List.of();
    }

    @Value.Default
    @JsonMerge(OptBoolean.FALSE)
    default List<String> getExcluded() {
      return List.of();
    }
  }

  @Value.Immutable
  @Value.Modifiable
  @JsonDeserialize(as = ModifiableHttpStatusConfiguration.class)
  interface HttpStatusConfiguration {
    @Value.Default
    @JsonMerge(OptBoolean.FALSE)
    default List<String> getIncluded() {
      return List.of("200");
    }

    @Value.Default
    @JsonMerge(OptBoolean.FALSE)
    default List<String> getExcluded() {
      return List.of();
    }
  }
}
