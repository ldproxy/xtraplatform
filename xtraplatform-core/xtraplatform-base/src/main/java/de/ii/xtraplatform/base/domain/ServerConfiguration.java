/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.ii.xtraplatform.docs.DocFile;
import io.dropwizard.server.DefaultServerFactory;

/**
 * @langEn # Web Server
 *     <p>## Port
 *     <p>The default port of the web server is `7080`. This can be changed, e.g. if there is a
 *     conflict with another application.
 *     <p><code>
 * ```yaml
 * server:
 *   applicationConnectors:
 *     - type: http
 *       port: 8080
 * ```
 * </code>
 *     <p>## External URL
 *     <p>If the application is run behind another web server, e.g. for HTTPS or to change the path
 *     where the services are accessible (`/rest/services`), the external URL must be configured.
 *     <p>A common use case would be to use *Apache HTTP Server* to set up a *ProxyPass* from
 *     `https://example.org/ldproxy` to `http://ldproxy-host:7080/rest/services`. Then the following
 *     would need to be configured:
 *     <p><code>
 * ```yaml
 * server:
 *   externalUrl: https://example.org/ldproxy/
 * ```
 * </code>
 *     <p>## Request-Logging
 *     <p>Request logging is disabled by default. This example would enable writing request logs to
 *     `data/log/requests.log`. It also enables daily log rotation and keeps old logs zipped for a
 *     week.
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
 * @langDe # Webserver
 *     <p>## Port
 *     <p>Der Standard-Port des Webservers ist `7080`. Dieser kann geändert werden, z.B. wenn es
 *     einen Konflikt mit einer anderen Anwendung gibt.
 *     <p><code>
 * ```yaml
 * server:
 *   applicationConnectors:
 *     - type: http
 *       port: 8080
 * ```
 * </code>
 *     <p>## Externe URL
 *     <p>Wenn die Applikation hinter einem weiteren Webserver betrieben wird, z.B. für HTTPS oder
 *     um den Pfad zu ändern, unter dem die Dienste erreichbar sind (`/rest/services`), muss die
 *     externe URL konfiguriert werden.
 *     <p>Ein verbreiteter Anwendungsfall wäre mittels *Apache HTTP Server* ein *ProxyPass* von
 *     `https://example.org/ldproxy` nach `http://ldproxy-host:7080/rest/services` einzurichten.
 *     Dann müsste folgendes konfiguriert werden:
 *     <p><code>
 * ```yaml
 * server:
 *   externalUrl: https://example.org/ldproxy/
 * ```
 * </code>
 *     <p>## Request-Logging
 *     <p>Request-Logging ist standardmäßig deaktiviert. Dieses Beispiel würde das Schreiben von
 *     Request-Logs nach `data/log/requests.log` aktivieren. Es aktiviert auch die tägliche
 *     Log-Rotation und verwahrt alte Logs gezippt für eine Woche.
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
 */
@DocFile(path = "application", name = "60-server.md")
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE, defaultImpl = ServerConfiguration.class)
public class ServerConfiguration extends DefaultServerFactory {

  private String externalUrl;

  /**
   * @en The [external URL](#external-url) of the web server.
   * @de Die [externe URL](#external-url) des Webservers
   * @default
   */
  @JsonProperty
  public String getExternalUrl() {
    return externalUrl;
  }

  @JsonProperty
  public void setExternalUrl(final String externalUrl) {
    this.externalUrl = externalUrl;
  }

  // needed because @JsonProperty for the getter is missing in AbstractServerFactory
  @JsonProperty
  public Boolean getRegisterDefaultExceptionMappers() {
    return super.getRegisterDefaultExceptionMappers();
  }
}
