/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.ii.xtraplatform.docs.DocFile;
import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.logging.LoggingFactory;
import io.dropwizard.server.ServerFactory;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * # Global configuration
 *
 * @langEn The configuration file `cfg.yml` is located in the data directory.
 * @langDe Die Konfigurationsdatei `cfg.yml` befindet sich im Daten-Verzeichnis.
 * @langEn ## Store structure Each configuration object has a type, an optional sub-type, and an id
 *     unique to the type (see [configuration-object-types](README.md#configuration-object-types)).
 *     The path to the corresponding configuration file then looks like this:
 *     <p><code>
 * ```text
 * store/entities/{type}/{id}.yml
 * ```
 * </code>
 *     <p>Defaults for all configuration objects of a type or sub-type can optionally be set in such
 *     files:
 *     <p><code>
 * ```text
 * store/defaults/{type}.yml
 * store/defaults/{type}.{sub-type}.yml
 * ```
 * </code>
 *     <p>You can also create overrides for configuration objects. These have e.g. the purpose to
 *     make environment specific adjustments without having to change the main configuration files.
 *     The path to the overrides looks like this:
 *     <p><code>
 * ```text
 * store/overrides/{type}/{id}.yml
 * ```
 * </code>
 *     <p>Defaults, configuration objects and overrides are read and merged in this order. This
 *     means that information in the configuration object overwrites information in defaults and
 *     information in overrides overwrites both information in defaults and in the configuration
 *     object.
 *     <p>The merging functions also for nested structures, i.e. one does not have to repeat data in
 *     the different files, but can set e.g. in Overrides only purposefully the data, which one
 *     wants to overwrite.
 *     <p>The merged configuration objects must contain then all obligation data, otherwise it comes
 *     with the start to an error.
 * @langDe ### Struktur des Store Jedes Konfigurationsobjekt hat einen Typ, einen optionalen Sub-Typ
 *     sowie eine für den Typ eindeutige Id (siehe
 *     [Konfigurationsobjekt-Typen](README.md#configuration-object-types)). Der Pfad zur
 *     entsprechenden Konfigurationsdatei sieht dann so aus:
 *     <p><code>
 * ```text
 * store/entities/{typ}/{id}.yml
 * ```
 * </code>
 *     <p>Defaults für alle Konfigurationsobjekte eines Typs oder Sub-Typs können optional in
 *     solchen Dateien gesetzt werden:
 *     <p><code>
 * ```text
 * store/defaults/{typ}.yml
 * store/defaults/{typ}.{sub-typ}.yml
 * ```
 * </code>
 *     <p>Außerdem kann man noch Overrides für Konfigurationsobjekte anlegen. Diese haben z.B. den
 *     Zweck, umgebungsspezifische Anpassungen vorzunehmen, ohne die Haupt-Konfigurationsdateien
 *     ändern zu müssen. Der Pfad zu den Overrides sieht so aus:
 *     <p><code>
 * ```text
 * store/overrides/{typ}/{id}.yml
 * ```
 * </code>
 *     <p>Defaults, Konfigurationsobjekte und Overrides werden in dieser Reihenfolge eingelesen und
 *     zusammengeführt. Das heißt Angaben im Konfigurationsobjekt überschreiben Angaben in Defaults
 *     und Angaben in Overrides überschreiben sowohl Angaben in Defaults als auch im
 *     Konfigurationsobjekt.
 *     <p>Das Zusammenführen funktioniert auch für verschachtelte Strukturen, d.h. man muss in den
 *     verschiedenen Dateien keine Angaben wiederholen, sondern kann z.B. in Overrides nur gezielt
 *     die Angaben setzen, die man überschreiben will.
 *     <p>Die zusammengeführten Konfigurationsobjekte müssen dann alle Pflichtangaben enthalten,
 *     ansonsten kommt es beim Start zu einem Fehler.
 * @langEn ### Additional directories For fixed predefined or standardized configuration objects it
 *     may make sense to make environment specific adjustments in a separate directory. One or more
 *     such directories can be configured with `additionalLocations`. The paths to be specified can
 *     be either absolute or relative to the data directory, e.g.:
 *     <p><code>
 * ```yml
 * store:
 *   additionalLocations:
 *     - env/test
 * ```
 * </code>
 *     <p>Such a directory can then again contain defaults and overrides, e.g.:
 *     <p><code>
 * ```text
 * env/test/defaults/{type}.yml
 * env/test/overrides/{type}/{id}.yml
 * ```
 * </code>
 *     <p>The merge order for all listed paths would then look like this:
 *     <p><code>
 * ```text
 * store/defaults/{type}.yml
 * store/defaults/{type}.{sub-type}.yml
 * env/test/defaults/{type}.yml
 * store/entities/{type}/{id}.yml
 * store/overrides/{type}/{id}.yml
 * env/test/overrides/{type}/{id}.yml
 * ```
 * </code>
 *     <p>
 * @langDe ### Zusätzliche Verzeichnisse Bei fest vordefinierten oder standardisierten
 *     Konfigurationsobjekten kann es Sinn machen, umgebungsspezifische Anpassungen in einem
 *     separaten Verzeichnis vorzunehmen. Ein oder mehrere solche Verzeichnisse können mit
 *     `additionalLocations` konfiguriert werden. Die anzugebenden Pfade können entweder absolut
 *     oder relativ zum Daten-Verzeichnis sein, also z.B.:
 *     <p><code>
 * ```yml
 * store:
 *   additionalLocations:
 *     - env/test
 * ```
 * </code>
 *     <p>Ein solches Verzeichnis kann dann wiederum Defaults und Overrides enthalten, also z.B.:
 *     <p><code>
 * ```text
 * env/test/defaults/{typ}.yml
 * env/test/overrides/{typ}/{id}.yml
 * ```
 * </code>
 *     <p>Die Reihenfolge der Zusammenführung für alle aufgeführten Pfade sähe dann so aus:
 *     <p><code>
 * ```text
 * store/defaults/{typ}.yml
 * store/defaults/{typ}.{sub-typ}.yml
 * env/test/defaults/{typ}.yml
 * store/entities/{typ}/{id}.yml
 * store/overrides/{typ}/{id}.yml
 * env/test/overrides/{typ}/{id}.yml
 * ``
 * </code>
 *     <p>
 * @langEn ### Splitting of defaults and overrides Defaults and overrides can be split into smaller
 *     files, e.g. to increase the clarity. The splitting follows the object structure in the
 *     configuration objects.
 *     <p><code>
 * ```yml
 * key1:
 *   key2:
 *     key3: value1
 * ```
 * </code>
 *     <p>To set a default or override for this value, the files described above could be used:
 *     <p><code>
 * ```text
 * store/defaults/{type}.{sub-type}.yml
 * store/overrides/{type}/{id}.yml
 * ```
 * </code>
 *     <p>However, separate files can be created for the object `key1` or the object `key2`, for
 *     example:
 *     <p><code>
 * ```text
 * store/defaults/{type}/{sub-type}/key1.yml
 * ```
 * </code>
 *     <p>
 *     <p><code>
 * ```yml
 * key2:
 *   key3: value2
 * ```
 * </code>
 *     <p>
 *     <p><code>
 * ```text
 * store/overrides/{type}/{id}/key1/key2.yml
 * ```
 * </code>
 *     <p>
 *     <p><code>
 * ```yml
 * key3: value3
 * ```
 * </code>
 *     <p>So the path of the object can be moved from the YAML to the file system, so to speak.
 *     <p>The order of merging follows the specificity of the path. For all paths listed, it would
 *     look like this:
 *     <p><code>
 * ```text
 * store/defaults/{typ}.{sub-typ}.yml
 * store/defaults/{typ}/{sub-typ}/key1.yml
 * store/entities/{typ}/{id}.yml
 * store/overrides/{typ}/{id}.yml
 * store/overrides/{typ}/{id}/key1/key2.yml
 * ```
 * </code>
 *     <p><a name="array-exceptions"></a>
 *     <p>There are some special cases where splitting is not only allowed based on object paths,
 *     but also e.g. for uniquely referenceable array elements. These special cases are discussed in
 *     the description of [configuration-object-types](README.md#configuration-object-types) in the
 *     ["special-cases"](README.md#special-cases) section.
 * @langDe ### Aufsplitten von Defaults und Overrides Defaults und Overrides können in kleinere
 *     Dateien aufgesplittet werden, z.B. um die Übersichtlichkeit zu erhöhen. Die Aufsplittung
 *     folgt dabei der Objektstruktur in den Konfigurationsobjekten.
 *     <p><code>
 * ```yml
 * key1:
 *   key2:
 *     key3: value1
 * ```
 * </code>
 *     <p>Um ein Default oder Override für diesen Wert zu setzen, könnten die oben beschriebenen
 *     Dateien verwendet werden:
 *     <p><code>
 * ```text
 * store/defaults/{typ}.{sub-typ}.yml
 * store/overrides/{typ}/{id}.yml
 * ```
 * </code>
 *     <p>Es können aber auch separate Dateien für das Objekt `key1` oder das Objekt `key2` angelegt
 *     werden, z.B.:
 *     <p><code>
 * ```text
 * store/defaults/{typ}/{sub-typ}/key1.yml
 * ```
 * </code>
 *     <p>
 *     <p><code>
 * ```yml
 * key2:
 *   key3: value2
 * ```
 * </code>
 *     <p>
 *     <p><code>
 * ```text
 * store/overrides/{typ}/{id}/key1/key2.yml
 * ```
 * </code>
 *     <p>
 *     <p><code>
 * ```yml
 * key3: value3
 * ```
 * </code>
 *     <p>Der Pfad des Objekts kann also sozusagen aus dem YAML ins Dateisystem verlagert werden.
 *     <p>Die Reihenfolge der Zusammenführung folgt der Spezifität des Pfads. Für alle aufgeführten
 *     Pfade sähe dann so aus:
 *     <p><code>
 * ```text
 * store/defaults/{typ}.{sub-typ}.yml
 * store/defaults/{typ}/{sub-typ}/key1.yml
 * store/entities/{typ}/{id}.yml
 * store/overrides/{typ}/{id}.yml
 * store/overrides/{typ}/{id}/key1/key2.yml
 * ```
 * </code>
 *     <p><a name="array-exceptions"></a>
 *     <p>Es gibt einige Sonderfälle, bei denen das Aufsplitten nicht nur anhand der Objektpfade
 *     erlaubt ist, sondern z.B. auch für eindeutig referenzierbare Array-Element. Auf diese
 *     Sonderfälle wird in der Beschreibung der
 *     [Konfigurationsobjekt-Typen](README.md#configuration-object-types) im Abschnitt
 *     ["Besonderheiten"](README.md#special-cases) eingegangen.
 * @langEn ### Environment variables Both in `cfg.yml` and in configuration objects, defaults and
 *     overrides, substitutions can be made by environment variables.
 *     <p>Such an expression `${NAME}` in one of these files is replaced by the value of the
 *     environment variable `NAME`. If the variable is not set, `null` is used. You can also specify
 *     a default value in case the variable is not set. This would look like `${NAME:-VALUE}`.
 *     <p>In `cfg.yml` you can use this mechanism e.g. to define an additional directory depending
 *     on an environment variable:
 *     <p><code>
 * ```yml
 * store:
 *   additionalLocations:
 *     - env/${DEPLOYMENT_ENV:-production}
 * ```
 * </code>
 *     <p>To load the files from the above example into `env/test`, you would then have to set the
 *     environment variable `DEPLOYMENT_ENV=test`. If this is not set, the directory
 *     `env/production` would be loaded.
 * @langDe ### Umgebungsvariablen Sowohl in der `cfg.yml` als auch in Konfigurationsobjekten,
 *     Defaults und Overrides können Ersetzungen durch Umgebungsvariablen vorgenommen werden.
 *     <p>Ein solcher Ausdruck `${NAME}` in einer dieser Dateien wird durch den Wert der
 *     Umgebungsvariable `NAME` ersetzt. Ist die Variable nicht gesetzt, wird `null` eingesetzt. Man
 *     kann auch einen Default-Wert angeben, für den Fall, dass die Variable nicht gesetzt ist. Das
 *     sähe dann so `${NAME:-WERT}` aus.
 *     <p>In der `cfg.yml` kann man diesen Mechanismus z.B. verwenden, um ein zusätzliches
 *     Verzeichnis anhängig von einer Umgebungsvariable zu definieren:
 *     <p><code>
 * ```yml
 * store:
 *   additionalLocations:
 *     - env/${DEPLOYMENT_ENV:-production}
 * ```
 * </code>
 *     <p>Um die Dateien aus obigem Beispiel in `env/test` zu laden, müsste man dann die
 *     Umgebunsvariable `DEPLOYMENT_ENV=test` setzen. Wenn diese nicht gesetzt ist würde das
 *     Verzeichnis `env/production` geladen.
 * @langEn ### External URL If the application is run behind another web server, e.g. for HTTPS or
 *     to change the path where the services are accessible (`/rest/services`), the external URL
 *     must be configured.
 *     <p>A common use case would be to use *Apache HTTP Server* to set up a *ProxyPass* from
 *     `https://example.org/ldproxy` to `http://ldproxy-host:7080/rest/services`. Then the following
 *     would need to be configured:
 *     <p><code>
 * ```yaml
 * server:
 *   externalUrl: https://example.org/ldproxy/
 * ```
 * </code>
 *     <p>
 * @langDe ### Externe URL Wenn die Applikation hinter einem weiteren Webserver betrieben wird, z.B.
 *     für HTTPS oder um den Pfad zu ändern, unter dem die Dienste erreichbar sind
 *     (`/rest/services`), muss die externe URL konfiguriert werden.
 *     <p>Ein verbreiteter Anwendungsfall wäre mittels *Apache HTTP Server* ein *ProxyPass* von
 *     `https://example.org/ldproxy` nach `http://ldproxy-host:7080/rest/services` einzurichten.
 *     Dann müsste folgendes konfiguriert werden:
 *     <p><code>
 * ```yaml
 * server:
 *   externalUrl: https://example.org/ldproxy/
 * ```
 * </code>
 *     <p>
 * @langEn ### Request-Logging Request logging is disabled by default. This example would enable
 *     writing request logs to `data/log/requests.log`. It also enables daily log rotation and keeps
 *     old logs zipped for a week.
 *     <p><code>
 * ```yaml
 * server:
 *   requestLog:
 *     type: classic
 *     timeZone: Europe/Berlin
 *     appenders:
 *       - type: file
 *         currentLogFilename: data/log/requests.log
 *         archive: true
 *         archivedLogFilenamePattern: data/log/requests-%d.zip
 *         archivedFileCount: 7
 * ```
 * </code>
 *     <p>
 * @langDe ### Request-Logging Request-Logging ist standardmäßig deaktiviert. Dieses Beispiel würde
 *     das Schreiben von Request-Logs nach `data/log/requests.log` aktivieren. Es aktiviert auch die
 *     tägliche Log-Rotation und verwahrt alte Logs gezippt für eine Woche.
 * @langEn ### Port The default port of the web server is `7080`. This can be changed, e.g. if there
 *     is a conflict with another application.
 *     <p><code>
 * ```yaml
 * server:
 *   applicationConnectors:
 *     - type: http
 *       port: 8080
 * ```
 * </code>
 *     <p>
 * @langDe ### Port Der Standard-Port des Webservers ist `7080`. Dieser kann geändert werden, z.B.
 *     wenn es einen Konflikt mit einer anderen Anwendung gibt.
 *     <p><code>
 * ```yaml
 * server:
 *   applicationConnectors:
 *     - type: http
 *       port: 8080
 * ```
 * </code>
 *     <p>
 * @langEn ### HTTP-Proxy If the application needs to use an HTTP proxy to access external
 *     resources, it can be configured as follows.
 *     <p>In this example, the HTTP proxy URL is `http://localhost:8888`. Connections to hosts
 *     listed under `nonProxyHosts` are made directly and not through the HTTP proxy. In this
 *     example, this would be `localhost`, any IP address starting with `192.168.` and any subdomain
 *     of `example.org`.
 *     <p><code>
 * ```yaml
 * httpClient:
 *   proxy:
 *     host: localhost
 *     port: 8888
 *     scheme : http
 *     nonProxyHosts:
 *       - localhost
 *       - '192.168.*'
 *       - '*.example.org'
 * ```
 * </code>
 *     <p>
 * @langDe ### HTTP-Proxy Falls die Applikation einen HTTP-Proxy verwenden muss, um auf externe
 *     Ressourcen zuzugreifen, kann dieser wie folgt konfiguriert werden.
 *     <p>In diesem Beispiel ist die HTTP-Proxy-URL `http://localhost:8888`. Verbindungen zu Hosts
 *     die unter `nonProxyHosts` augelistet sind werden direkt und nicht durch den HTTP-Proxy
 *     hergestellt. In diesem Beispiel wären das `localhost`, jede IP-Addresse die mit `192.168.`
 *     anfängt und jede Subdomain von `example.org`.
 *     <p><code>
 * ```yaml
 * httpClient:
 *   proxy:
 *     host: localhost
 *     port: 8888
 *     scheme : http
 *     nonProxyHosts:
 *       - localhost
 *       - '192.168.*'
 *       - '*.example.org'
 * ```
 * </code>
 *     <p>
 * @langEn ### Idle-Timeout This setting should only be adjusted if users report persistent problems
 *     with long-running requests. In most cases, the default setting of 30 seconds is recommended.
 * @langDe ### Idle-Timeout Diese Einstellung sollte nur angepasst werden, falls Nutzer von
 *     anhaltenden Problemen mit langlaufenden Requests berichten. In den meisten Fällen wird die
 *     Standard-Einstellung von 30 Sekunden empfohlen.
 * @langEn ### Log-Level The log level for the application is `INFO` by default. Other possible
 *     values are `OFF`, `ERROR` and `WARN`. For debugging it can be set to `DEBUG` for example:
 *     <p><code>
 * ```yaml
 * logging:
 *   level: DEBUG
 * ```
 * </code>
 *     <p>
 * @langDe ### Log-Level Der Log-Level für die Applikation ist standardmäßig `INFO`. Weitere
 *     mögliche Werte sind `OFF`, `ERROR` und `WARN`. Für die Fehlersuche kann er zum Beispiel auf
 *     `DEBUG` gesetzt werden:
 *     <p><code>
 * ```yaml
 * logging:
 *   level: DEBUG
 * ```
 * </code>
 *     <p>
 * @langEn ### Log output By default, application logs are written to `data/log/xtraplatform.log`.
 *     Daily log rotation is enabled and old logs are zipped and kept for a week. The log file or
 *     rotation settings can be changed:
 *     <p><code>
 * ```yaml
 * logging:
 *   appenders:
 *     - type: file
 *       currentLogFilename: /var/log/ldproxy.log
 *       archive: true
 *       archivedLogFilenamePattern: /var/log/ldproxy-%d.zip
 *       archivedFileCount: 30
 *       timeZone: Europe/Berlin
 * ```
 * </code>
 *     <p>
 * @langDe ### Log-Ausgabe Standardmäßig werden Applikations-Logs nach `data/log/xtraplatform.log`
 *     geschrieben. Die tägliche Log-Rotation ist aktiviert und alte Logs werden gezippt und für
 *     eine Woche verwahrt. Die Log-Datei oder die Rotations-Einstellungen können geändert werden:
 *     <p><code>
 * ```yaml
 * logging:
 *   appenders:
 *     - type: file
 *       currentLogFilename: /var/log/ldproxy.log
 *       archive: true
 *       archivedLogFilenamePattern: /var/log/ldproxy-%d.zip
 *       archivedFileCount: 30
 *       timeZone: Europe/Berlin
 * ```
 * </code>
 *     <p>
 * @see StoreConfiguration
 * @see ServerConfiguration
 * @see LoggingConfiguration
 * @see io.dropwizard.client.HttpClientConfiguration
 * @see ManagerConfiguration
 * @see ProjConfiguration
 * @see BackgroundTasksConfiguration
 * @see AuthConfig
 */
@DocFile(path = "configuration", name="global.md")
public class AppConfiguration extends Configuration {

  @Valid @NotNull private ServerConfiguration server;
  @Valid @NotNull private LoggingConfiguration logging;
  // TODO: not used anymore, but removing breaks backwards compatibility
  @Deprecated @JsonProperty public boolean useFormattedJsonOutput;
  @Deprecated @JsonProperty public boolean allowServiceReAdding;
  @Valid @NotNull private HttpClientConfiguration httpClient;
  @Valid @NotNull @JsonProperty public StoreConfiguration store;
  @Valid @NotNull @JsonProperty public AuthConfig auth;
  @Valid @NotNull @JsonProperty public ManagerConfiguration manager;
  @Valid @NotNull @JsonProperty public BackgroundTasksConfiguration backgroundTasks;
  @Valid @NotNull @JsonProperty public ProjConfiguration proj;
  @Valid @JsonProperty public ClusterConfiguration cluster;

  public AppConfiguration() {
    this.logging = new LoggingConfiguration();
    this.server = new ServerConfiguration();
    this.httpClient = new HttpClientConfiguration();
    this.store = new StoreConfiguration();
    this.auth = new AuthConfig();
    this.manager = new ManagerConfiguration();
    this.backgroundTasks = new BackgroundTasksConfiguration();
    this.proj = new ProjConfiguration();
  }

  public AppConfiguration(boolean noInit) {}

  @Override
  @JsonProperty("server")
  public ServerConfiguration getServerFactory() {
    return server;
  }

  @Override
  @JsonIgnore
  public void setServerFactory(ServerFactory factory) {}

  @JsonProperty("server")
  public void setServerFactory(ServerConfiguration factory) {
    this.server = factory;
  }

  @Override
  @JsonIgnore
  public synchronized LoggingFactory getLoggingFactory() {
    return logging;
  }

  @JsonProperty("logging")
  public synchronized LoggingConfiguration getLoggingConfiguration() {
    return logging;
  }

  @Override
  @JsonIgnore
  public synchronized void setLoggingFactory(LoggingFactory factory) {}

  @JsonProperty("logging")
  public synchronized void setLoggingFactory(LoggingConfiguration factory) {
    this.logging = factory;
  }

  @JsonProperty("httpClient")
  public HttpClientConfiguration getHttpClient() {
    return httpClient;
  }

  @JsonProperty("httpClient")
  public void setHttpClient(HttpClientConfiguration httpClient) {
    this.httpClient = httpClient;
  }
}
