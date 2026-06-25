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
 * @langEn # Audit Logging
 *     <p>Audit Logging can be used to automatically create and save audit logs for API requests.
 *     The configuration is split across three levels:
 *     <p><code>
 *  - Global level, which is described here. The global configuration applies to all APIs.
 *  - API level, which is explained in more detail at [API configuration](../../services/#audit-logging). The API-level configuration is used to fine-tune the configuration for a specific API.
 *  - Provider level, which consists of the schema option `audit` that is described in more detail at [Schema Definitions](../../providers/feature/#schema-definitions). It is used to specify which `properties` to log.
 *  </code>
 *     <p>Below is a detailed description of the global configuration, notes on storage and an
 *     example containing the relevant pieces from all configuration levels.
 *     <p>## Options
 *     <p>{@docTable:properties}
 *     <p>## Storage
 *     <p>The log entries are stored in the resource store in the `logs/audit` directory. The file
 *     name matches the request ID from the application log.
 *     <p>## Examples
 *     <p>In the following, examples are shown for the global, API and provider configuration, as
 *     well as an audit log example that is produced from the configs. The
 *     [Vineyards](https://demo.ldproxy.net/vineyards)-API from the
 *     [demos](https://demo.ldproxy.net/) has been used here.
 *     <p>Global config: <code>
 * ```yml
 * auditLog:
 *   enabled: true
 *   retries: 3
 *   type: JSON_PRETTY
 *   pathPrefix: "mysubdirectory/{api}/{date}"
 *   headers:
 *     included: [ "User-Agent", "Host" ]
 *     excluded: [ ]
 *   claims:
 *     included: [ "realm_access", "resource_access" ]
 *     excluded: [ ]
 *   httpStatus:
 *     included: [ "200" ]
 *     excluded: [ ]
 * ```
 *     </code>
 *     <p>API config: <code>
 * ```yml
 * auditLog:
 *   enabled: true
 *   operations:
 *     - "data:read::vineyards"
 *     - "write"
 * ```
 *     </code>
 *     <p>Provider config: <code>
 * ```yml
 * types:
 *   vineyards:
 *     sourcePath: /Weinlagen
 *     type: OBJECT
 *     properties:
 *       registerId:
 *         sourcePath: wlg_nr
 *         type: INTEGER
 *         role: ID
 *         label: Vineyard register number
 *         audit: true
 *       name:
 *         sourcePath: wlg_name
 *         type: STRING
 *         label: Vineyard name
 *         audit: true
 * ```
 *     </code>
 *     <p>As a result of the configs above, the following audit log could be produced and saved as
 *     `logs/audit/mysubdirectory/vineyards/2026-06-15/48f4923c-b52c-4dfb-b45e-3e892995a473.json`:
 * @langDe # Audit Logging
 *     <p>Audit Logging kann verwendet werden, um automatisch Audit-Logs für API-Anfragen zu
 *     erstellen und zu speichern. Die Konfiguration ist auf drei Ebenen aufgeteilt:
 *     <p><code>
 *  - Globale Ebene, die hier beschrieben wird. Die globale Konfiguration gilt für alle APIs.
 *  - API-Ebene, die unter [API-Konfiguration](../../services/#audit-logging) ausführlicher beschrieben wird. Die Konfiguration auf API-Ebene dient dazu, die Konfiguration für eine bestimmte API zu verfeinern.
 *  - Provider-Ebene, die aus der Schema-Option `audit` besteht, die unter [Schema-Definitionen](../../providers/feature/#schema-definitionen) ausführlicher beschrieben wird. Sie wird verwendet, um anzugeben, welche `properties` geloggt werden sollen.
 *  </code>
 *     <p>Nachfolgend ist eine detaillierte Beschreibung der globalen Konfigurationsoptionen,
 *     Hinweise zur Speicherung und ein Beispiel, das die relevanten Teile aller
 *     Konfigurationsebenen enthält.
 *     <p>## Optionen
 *     <p>{@docTable:properties}
 *     <p>## Speicherung
 *     <p>Die Log-Einträge werden im Ressourcen-Store im Verzeichnis `logs/audit` abgelegt. Der
 *     Dateiname entspricht der Request-ID aus dem Anwendungsprotokoll.
 *     <p>## Beispiele
 *     <p>Im Folgenden werden Beispiele für die globale, API- und Provider-Konfiguration gezeigt
 *     sowie ein Audit-Log-Beispiel, das sich aus diesen Konfigurationen ergeben kann. Hier wurde
 *     die [Vineyards](https://demo.ldproxy.net/vineyards)-API aus den
 *     [Demos](https://demo.ldproxy.net/) verwendet.
 *     <p>Globale Konfiguration: <code>
 * ```yml
 * auditLog:
 *   enabled: true
 *   retries: 3
 *   type: JSON_PRETTY
 *   pathPrefix: "mysubdirectory/{api}/{date}"
 *   headers:
 *     included: [ "User-Agent", "Host" ]
 *     excluded: [ ]
 *   claims:
 *     included: [ "realm_access", "resource_access" ]
 *     excluded: [ ]
 *   httpStatus:
 *     included: [ "200" ]
 *     excluded: [ ]
 * ```
 *     </code>
 *     <p>API-Konfiguration: <code>
 * ```yml
 * auditLog:
 *   enabled: true
 *   operations:
 *     - "data:read::vineyards"
 *     - "write"
 * ```
 *     </code>
 *     <p>Provider-Konfiguration: <code>
 * ```yml
 * types:
 *   vineyards:
 *     sourcePath: /Weinlagen
 *     type: OBJECT
 *     properties:
 *       registerId:
 *         sourcePath: wlg_nr
 *         type: INTEGER
 *         role: ID
 *         label: Vineyard register number
 *         audit: true
 *       name:
 *         sourcePath: wlg_name
 *         type: STRING
 *         label: Vineyard name
 *         audit: true
 * ```
 *     </code>
 *     <p>Als Ergebnis der obigen Konfigurationen entsteht beispielsweise das folgende Audit-Log,
 *     das als
 *     `logs/audit/mysubdirectory/vineyards/2026-06-15/48f4923c-b52c-4dfb-b45e-3e892995a473.json`
 *     gespeichert werden würde:
 * @langAll <code>
 * ```json
 * {
 *   "id" : "48f4923c-b52c-4dfb-b45e-3e892995a473",
 *   "started" : "2026-06-15T08:27:05.372819295Z",
 *   "finished" : "2026-06-15T08:27:05.416584477Z",
 *   "api" : "vineyards",
 *   "actor" : {
 *     "type" : "USER",
 *     "id" : "johndoe",
 *     "claims" : {
 *       "realm_access" : {
 *         "roles" : [
 *           "offline_access",
 *           "authorization"
 *         ]
 *       },
 *       "resource_access" : {
 *         "roles" : [
 *           "read",
 *           "manage-account",
 *           "manage-account-links",
 *           "view-profile"
 *         ]
 *       }
 *     }
 *   },
 *   "operation" : {
 *     "method" : "GET",
 *     "path" : "/collections/vineyards/items",
 *     "headers" : {
 *       "User-Agent" : "Mozilla/5.0 (X11; Linux x86_64; rv:140.0) Gecko/20100101 Firefox/140.0",
 *       "Host" : "localhost:7080"
 *     },
 *     "parameter" : {
 *       "f" : "json"
 *     },
 *     "status" : "200"
 *   },
 *   "target" : {
 *     "features" : [
 *       {
 *         "id" : "460258",
 *         "name" : "Kupp"
 *       },
 *       {
 *         "id" : "511109",
 *         "name" : "Höll"
 *       }
 *     ]
 *   }
 * }
 * ```
 * </code>
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
   *     `{api}/foo/{date}/bar` would be stored at `logs/audit/vineyards/foo/2026-06-03/bar`.
   * @langDe Gibt den Pfad an, der der Log-Datei vorangestellt werden soll. Dabei werden `{api}` und
   *     `{date}` jeweils mit der API-ID bzw. dem ISO-Datum der Anfrage ersetzt. Beispielsweise
   *     könnten die Log-Dateien für `{api}/foo/{date}/bar` unter
   *     `logs/audit/vineyards/foo/2026-06-03/bar` gespeichert werden.
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
   *     gibt an, welche Header aus `included` nicht geloggt werden sollen. Der spezielle Wert `*`
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
